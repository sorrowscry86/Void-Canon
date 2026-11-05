/** Personal development image service. No provider credentials are sent to Android. */
export const MODEL = '@cf/black-forest-labs/flux-2-klein-4b';
export const DAILY_BUDGET = 5000;
const MAX_BODY = 700000;

class PublicError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
const fail = (status, message) => { throw new PublicError(status, message); };
const json = (body, status = 200) => Response.json(body, {
  status, headers: { 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' },
});
function day() { return new Date().toISOString().slice(0, 10); }

async function authorized(request, expected) {
  if (!expected) return false;
  const supplied = request.headers.get('Authorization')?.replace(/^Bearer /, '') ?? '';
  if (supplied.length > 512) return false;
  const encode = new TextEncoder();
  const [a, b] = await Promise.all([supplied, expected].map(value =>
    crypto.subtle.digest('SHA-256', encode.encode(value))));
  return crypto.subtle.timingSafeEqual(a, b);
}

async function boundedJson(request) {
  if (!request.headers.get('Content-Type')?.startsWith('application/json')) fail(415, 'Send a JSON request.');
  if (Number(request.headers.get('Content-Length')) > MAX_BODY) fail(413, 'The reference image is too large.');
  const reader = request.body?.getReader();
  if (!reader) fail(400, 'The request is empty.');
  const chunks = []; let total = 0;
  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      total += value.byteLength;
      if (total > MAX_BODY) { await reader.cancel(); fail(413, 'The reference image is too large.'); }
      chunks.push(value);
    }
  } finally { reader.releaseLock(); }
  const bytes = new Uint8Array(total); let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
  try { return JSON.parse(new TextDecoder().decode(bytes)); }
  catch { fail(400, 'The request is not valid JSON.'); }
}

/** References use PNG so their bounded dimensions can be verified before inference. */
function referenceBytes(encoded) {
  if (typeof encoded !== 'string' || encoded.length > 650000 || !/^[A-Za-z0-9+/]*={0,2}$/.test(encoded))
    fail(400, 'Choose one smaller reference image.');
  let bytes;
  try { bytes = Uint8Array.from(atob(encoded), c => c.charCodeAt(0)); }
  catch { fail(400, 'The reference image is unreadable.'); }
  const signature = [137, 80, 78, 71, 13, 10, 26, 10];
  if (bytes.length < 33 || signature.some((n, i) => bytes[i] !== n) ||
      String.fromCharCode(...bytes.slice(12, 16)) !== 'IHDR') fail(400, 'The reference must be PNG.');
  const view = new DataView(bytes.buffer);
  const width = view.getUint32(16), height = view.getUint32(20);
  if (width < 1 || height < 1 || width > 448 || height > 448) fail(400, 'References must be at most 448 pixels per side.');
  return bytes;
}

function validate(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) fail(400, 'Invalid image request.');
  const { requestId, prompt, size, reference } = input;
  if (typeof requestId !== 'string' || !/^[a-zA-Z0-9-]{20,80}$/.test(requestId)) fail(400, 'Invalid request ID.');
  if (typeof prompt !== 'string' || !prompt.trim() || prompt.length > 2000) fail(400, 'Use a prompt of 1–2,000 characters.');
  const dimensions = { portrait: [512, 512], large: [1024, 1024], background: [1024, 576] }[size];
  if (!dimensions) fail(400, 'Choose a supported image size.');
  const bytes = reference == null ? null : referenceBytes(reference);
  // Whole tiles rounded on each axis, rounded up to reserve a conservative integer budget.
  const cost = Math.ceil(26.05 * Math.ceil(dimensions[0] / 512) * Math.ceil(dimensions[1] / 512) + (bytes ? 5.37 : 0));
  return { requestId, prompt: prompt.trim(), dimensions, bytes, cost };
}

export default {
  async fetch(request, env) {
    const route = new URL(request.url).pathname;
    try {
      if (route === '/health' && request.method === 'GET') return json({ service: 'Void Canon images', version: 1 });
      if (!await authorized(request, env.APP_ACCESS_TOKEN)) fail(401, 'Import your image connection file again.');
      if (route === '/v1/status' && request.method === 'GET') {
        const row = await env.DB.prepare('SELECT reserved FROM daily_budget WHERE day=?').bind(day()).first();
        return json({ model: MODEL, day: day(), reserved: row?.reserved ?? 0, limit: DAILY_BUDGET });
      }
      if (route !== '/v1/images' || request.method !== 'POST') fail(404, 'This image action is unavailable.');
      const { requestId, prompt, dimensions, bytes, cost } = validate(await boundedJson(request));
      try {
        await env.DB.prepare('INSERT INTO image_requests(id,day,cost) VALUES(?,?,?)').bind(requestId, day(), cost).run();
      } catch (error) {
        const message = String(error);
        if (message.includes('DAILY_IMAGE_LIMIT')) fail(429, 'The development image budget is used up. It resets at 00:00 UTC.');
        if (message.includes('UNIQUE constraint')) fail(409, 'This request was already submitted. Check your preview before explicitly generating another image.');
        throw error;
      }
      // Reservation remains consumed on failures/timeouts: the provider may have performed inference.
      let result;
      const started = Date.now();
      try {
        const form = new FormData();
        form.append('prompt', prompt);
        form.append('width', String(dimensions[0])); form.append('height', String(dimensions[1]));
        if (bytes) form.append('input_image_0', new Blob([bytes], { type: 'image/png' }), 'reference.png');
        const multipart = new Response(form);
        result = await env.AI.run(MODEL, { multipart: { body: multipart.body, contentType: multipart.headers.get('Content-Type') } });
        if (typeof result?.image !== 'string' || result.image.length > 12000000) throw new Error('Invalid provider image');
        const mime = result.image.startsWith('/9j/') ? 'image/jpeg' : result.image.startsWith('iVBOR') ? 'image/png' : null;
        if (!mime) throw new Error('Invalid provider format');
        await env.DB.prepare("UPDATE image_requests SET status='complete' WHERE id=?").bind(requestId).run();
        console.log(JSON.stringify({ action: 'image_complete', requestId, model: MODEL, milliseconds: Date.now() - started, reserved: cost }));
        return json({ image: result.image, mime, model: MODEL, requestId, reservedNeurons: cost });
      } catch {
        await env.DB.prepare("UPDATE image_requests SET status='failed' WHERE id=?").bind(requestId).run();
        console.log(JSON.stringify({ action: 'image_failed', requestId }));
        fail(502, 'Cloudflare could not finish this image. Your local images are safe. Retry explicitly when ready.');
      }
    } catch (error) {
      if (error instanceof PublicError) return json({ error: error.message }, error.status);
      console.log(JSON.stringify({ action: 'image_service_error' }));
      return json({ error: 'Image generation is temporarily unavailable. Retry explicitly when ready.' }, 503);
    }
  },
};
