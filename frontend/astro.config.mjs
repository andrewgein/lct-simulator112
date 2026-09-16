// @ts-check
import { defineConfig } from 'astro/config';

import node from '@astrojs/node';
import lit from '@awesome.me/astro-lit';
import preact from '@astrojs/preact';

// https://astro.build/config
export default defineConfig({
  adapter: node({
      mode: 'standalone'
  }),

  vite: {
      optimizeDeps: {
          include: ['astro-leaflet > leaflet'],
      }
  },
  integrations: [lit(), preact()],
});
