import { defineConfig } from 'vite';
import { readFileSync } from 'fs';
import { resolve } from 'path';

export default defineConfig({
  server: {
    port: 3000,
    headers: {
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'require-corp',
    },
  },
  worker: {
    rollupOptions: {
      external: ['/randomx-web.js'],
    },
  },
  plugins: [
    {
      name: 'serve-randomx-web',
      resolveId(id) {
        if (id === '/randomx-web.js') {
          return id;
        }
      },
      load(id) {
        if (id === '/randomx-web.js') {
          const pkgPath = resolve(__dirname, 'node_modules/randomx.js/dist/web/index.js');
          return readFileSync(pkgPath, 'utf-8');
        }
      },
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url === '/randomx-web.js') {
            const pkgPath = resolve(__dirname, 'node_modules/randomx.js/dist/web/index.js');
            try {
              const content = readFileSync(pkgPath);
              res.setHeader('Content-Type', 'application/javascript');
              res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
              res.setHeader('Cross-Origin-Embedder-Policy', 'require-corp');
              res.end(content);
            } catch (e) {
              res.statusCode = 404;
              res.end(`File not found: ${pkgPath}`);
            }
          } else {
            next();
          }
        });
      },
    },
  ],
});
