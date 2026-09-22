// @ts-check
import { defineConfig } from 'astro/config';

import node from '@astrojs/node';
import lit from '@awesome.me/astro-lit';
import preact from '@astrojs/preact';

const apiGatewayEndpoint = process.env.PUBLIC_API_ENDPOINT || 'http://127.0.0.1:8080';

function cookieValue(cookieHeader, name) {
  const cookie = cookieHeader
    ?.split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(`${name}=`));
  return cookie ? decodeURIComponent(cookie.slice(name.length + 1)) : null;
}

// https://astro.build/config
export default defineConfig({
  adapter: node({
      mode: 'standalone'
  }),

  vite: {
      optimizeDeps: {
          include: ['astro-leaflet > leaflet'],
      },
      server: {
          proxy: {
              '/api/v1/dialog': {
                  target: apiGatewayEndpoint,
                  ws: true,
                  configure(proxy) {
                      proxy.on('proxyReqWs', (proxyRequest, request) => {
                          const accessToken = cookieValue(request.headers.cookie, 'accessToken');
                          if (accessToken) {
                              proxyRequest.setHeader('Authorization', `Bearer ${accessToken}`);
                          }
                      });
                  },
              },
          },
      },
  },

  integrations: [lit(), preact()],
});
