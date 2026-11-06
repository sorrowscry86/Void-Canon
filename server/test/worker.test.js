import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { Miniflare, convertV4MiniflareOptions } from 'miniflare';

let mf;
const token = 'test-device-access-token';
const fixture = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg==';
before(async () => {
  const source = await readFile(new URL('../worker.js', import.meta.url), 'utf8');
  mf = new Miniflare(convertV4MiniflareOptions({ compatibilityDate: '2026-11-04',
    modules: [{ type: 'ESModule', path: 'entry.js', contents: `import worker from './worker.js';
      export default { fetch(request,env) { return worker.fetch(request,{...env,AI:{run:async(model,input)=> {
        const form=await new Response(input.multipart.body,{headers:{'Content-Type':input.multipart.contentType}}).formData();
        if(form.get('prompt')==='fail-provider')throw Error('private provider details');
        return {image:'${fixture}'};
      }}}); } };` }, { type: 'ESModule', path: 'worker.js', contents: source }],
    d1Databases: ['DB'], bindings: { APP_ACCESS_TOKEN: token },
  }));
  await (await mf.getD1Database('DB')).exec((await readFile(new URL('../schema.sql', import.meta.url), 'utf8')).replace(/\n/g, ' '));
});
after(async () => { await mf?.dispose(); });
const submit = (id, body = {}, auth = token) => mf.dispatchFetch('https://example.com/v1/images', {
  method: 'POST', headers: { Authorization: `Bearer ${auth}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ requestId: id, prompt: 'A harbor', size: 'portrait', ...body }),
});
const id = n => `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;

test('authentication and validation never spend image budget', async () => {
  assert.equal((await submit(id(1), {}, 'wrong')).status, 401);
  assert.equal((await submit(id(2), { prompt: '' })).status, 400);
  assert.equal((await submit(id(3), { size: 'unbounded' })).status, 400);
  assert.equal((await submit(id(4), { reference: 'bad' })).status, 400);
  assert.equal((await (await mf.getD1Database('DB')).prepare('SELECT COUNT(*) AS count FROM image_requests').first()).count, 0);
});
test('real multipart request returns an image; duplicates do not generate twice', async () => {
  const r = await submit(id(5), { reference: fixture });
  assert.equal(r.status, 200);
  const data = await r.json();
  assert.equal(data.mime, 'image/png'); assert.equal(data.reservedNeurons, 32);
  assert.equal(data.image, fixture);
  assert.equal((await submit(id(5))).status, 409);
  const status = await mf.dispatchFetch('https://example.com/v1/status', { headers: { Authorization: `Bearer ${token}` } });
  assert.equal((await status.json()).reserved, 32);
});
test('a provider failure retains the reservation and hides private diagnostics', async () => {
  const r = await submit(id(6), { prompt: 'fail-provider' });
  assert.equal(r.status, 502); assert.ok(!(await r.text()).includes('private provider details'));
  assert.equal((await (await mf.getD1Database('DB')).prepare('SELECT status FROM image_requests WHERE id=?').bind(id(6)).first()).status, 'failed');
});
test('concurrent requests cannot exceed the persistent daily budget', async () => {
  const db = await mf.getD1Database('DB');
  await db.prepare("UPDATE daily_budget SET reserved=4970 WHERE day=?").bind(new Date().toISOString().slice(0,10)).run();
  const responses = await Promise.all([submit(id(7)), submit(id(8)), submit(id(9))]);
  assert.equal(responses.filter(r => r.status === 200).length, 1);
  assert.equal(responses.filter(r => r.status === 429).length, 2);
  assert.equal((await db.prepare('SELECT reserved FROM daily_budget').first()).reserved, 4997);
});
