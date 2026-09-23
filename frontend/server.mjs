import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(fileURLToPath(new URL('./dist/', import.meta.url)));
const port = Number(process.env.PORT || 3000);
const backend = new URL(process.env.SHOP_API_URL || 'http://127.0.0.1:8080');
const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.svg': 'image/svg+xml', '.jpg': 'image/jpeg', '.png': 'image/png', '.webp': 'image/webp' };
const allowedApi = /^\/(products(?:\/\d+|\/search|\/filter)?|categories|users(?:\/me|\/\d+(?:\/role)?)?|auth\/login|orders(?:\/admin|\/\d+(?:\/cancel|\/status)?)?)$/;

export const server = http.createServer(async (req, res) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');
  try {
    const url = new URL(req.url, `http://127.0.0.1:${port}`);
    if (url.pathname.startsWith('/api/')) {
      const route = url.pathname.slice(4);
      res.setHeader('Cache-Control', 'no-store');
      if (!allowedApi.test(route)) return json(res, 404, { message: 'Маршрут не найден' });
      const origin = req.headers.origin;
      if (origin && ![`http://localhost:${port}`, `http://127.0.0.1:${port}`].includes(origin)) return json(res, 403, { message: 'Недопустимый источник запроса' });
      const chunks = [];
      let size = 0;
      for await (const chunk of req) {
        size += chunk.length;
        if (size > 1_000_000) return json(res, 413, { message: 'Слишком большой запрос' });
        chunks.push(chunk);
      }
      const headers = { Accept: 'application/json' };
      if (req.headers.authorization) headers.Authorization = req.headers.authorization;
      if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];
      try {
        const response = await fetch(new URL(route + url.search, backend), {
          method: req.method, headers,
          body: ['GET', 'HEAD'].includes(req.method) ? undefined : Buffer.concat(chunks),
          signal: AbortSignal.timeout(12000), redirect: 'manual'
        });
        res.writeHead(response.status, { 'Content-Type': response.headers.get('content-type') || 'application/json' });
        return res.end(Buffer.from(await response.arrayBuffer()));
      } catch {
        return json(res, 503, { message: 'Магазин временно недоступен. Попробуйте подключиться снова.', code: 'API_UNAVAILABLE' });
      }
    }
    if (!['GET', 'HEAD'].includes(req.method)) return json(res, 405, { message: 'Метод не поддерживается' });
    const relative = decodeURIComponent(url.pathname === '/' ? '/index.html' : url.pathname);
    const file = path.resolve(root, '.' + relative);
    if (!file.startsWith(root + path.sep)) return json(res, 403, { message: 'Нет доступа' });
    const contents = await readFile(file);
    res.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream', 'Cache-Control': 'no-cache' });
    res.end(req.method === 'HEAD' ? undefined : contents);
  } catch {
    json(res, 404, { message: 'Страница не найдена' });
  }
});

function json(res, status, data) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(data));
}

server.listen(port, process.env.HOST || '127.0.0.1', () => console.log(`Shop frontend: http://localhost:${port}`));
