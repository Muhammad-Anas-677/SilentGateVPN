package com.silentgatevpn.app.ui

/** Same content as worker/worker.js, shown in the Profiles guide. */
const val WORKER_SCRIPT = """
// SilentGate VPN - optional free Cloudflare Worker relay (VLESS over WebSocket, TCP only).
// 1. Create a Worker in the Cloudflare dashboard (free plan) and paste this file.
// 2. Replace UUID with your own (generate one at uuidgenerator.net).
// 3. In the app: Profiles > Add, paste the vless:// link shown in the app guide.
// NOTE: untested template. Cloudflare's terms may restrict proxying through Workers; use at your own risk.
import { connect } from 'cloudflare:sockets';

const UUID = 'REPLACE-WITH-YOUR-UUID';
const PROXY_IP = ''; // optional fallback relay host, leave empty if unused

export default {
  async fetch(request) {
    if (request.headers.get('Upgrade') !== 'websocket') {
      return new Response('OK', { status: 200 });
    }
    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];
    server.accept();
    handle(server, request.headers.get('sec-websocket-protocol') || '');
    return new Response(null, { status: 101, webSocket: client });
  },
};

function fromBase64Url(s) {
  if (!s) return null;
  s = s.replace(/-/g, '+').replace(/_/g, '/');
  try { return Uint8Array.from(atob(s), (c) => c.charCodeAt(0)); } catch (e) { return null; }
}

function parseVless(v) {
  if (v.length < 24) throw new Error('short header');
  const id = Array.from(v.slice(1, 17)).map((b) => b.toString(16).padStart(2, '0')).join('');
  if (id !== UUID.replace(/-/g, '').toLowerCase()) throw new Error('bad uuid');
  let i = 18 + v[17];
  if (v[i++] !== 1) throw new Error('tcp only');
  const port = (v[i] << 8) | v[i + 1];
  i += 2;
  const type = v[i++];
  let host;
  if (type === 1) { host = Array.from(v.slice(i, i + 4)).join('.'); i += 4; }
  else if (type === 2) { const l = v[i++]; host = new TextDecoder().decode(v.slice(i, i + l)); i += l; }
  else if (type === 3) {
    const parts = [];
    for (let k = 0; k < 8; k++) parts.push(((v[i + k * 2] << 8) | v[i + k * 2 + 1]).toString(16));
    host = parts.join(':'); i += 16;
  } else throw new Error('bad address type');
  return { host, port, version: v[0], payload: v.slice(i) };
}

async function openSocket(host, port) {
  try {
    const s = connect({ hostname: host, port });
    await s.opened;
    return s;
  } catch (e) {
    if (!PROXY_IP) throw e;
    const s = connect({ hostname: PROXY_IP, port });
    await s.opened;
    return s;
  }
}

function handle(ws, earlyData) {
  let writer = null;
  let ready = false;
  const queue = [];

  async function onFirst(data) {
    const req = parseVless(new Uint8Array(data));
    const socket = await openSocket(req.host, req.port);
    writer = socket.writable.getWriter();
    ready = true;
    if (req.payload.length) await writer.write(req.payload);
    let first = true;
    socket.readable.pipeTo(new WritableStream({
      write(chunk) {
        if (first) {
          first = false;
          const out = new Uint8Array(2 + chunk.length);
          out[0] = req.version; out[1] = 0; out.set(chunk, 2);
          ws.send(out);
        } else ws.send(chunk);
      },
      close() { try { ws.close(); } catch (e) {} },
      abort() { try { ws.close(); } catch (e) {} },
    })).catch(() => { try { ws.close(); } catch (e) {} });
    for (const q of queue) await writer.write(q);
    queue.length = 0;
  }

  let started = false;
  function onData(data) {
    if (!started) {
      started = true;
      onFirst(data).catch(() => { try { ws.close(); } catch (e) {} });
    } else if (ready) {
      writer.write(data).catch(() => {});
    } else {
      queue.push(data);
    }
  }

  const early = fromBase64Url(earlyData);
  if (early && early.length) onData(early.buffer);
  ws.addEventListener('message', (e) => onData(e.data));
  ws.addEventListener('close', () => { try { writer && writer.close(); } catch (e) {} });
  ws.addEventListener('error', () => { try { writer && writer.close(); } catch (e) {} });
}
"""
