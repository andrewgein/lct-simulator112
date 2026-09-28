import { apiCall } from '../../../../../services/ApiClient';

export const prerender = false;

export async function POST({ request, cookies }) {
  if (!['ADMIN', 'SUPERVISOR'].includes(cookies.get('role')?.value)) {
    return Response.json({ message: 'Недостаточно прав' }, { status: 403 });
  }
  try {
    const body = await request.json();
    const response = await apiCall('/api/v1/incidents/generate/stream', 'POST', body, cookies.get('accessToken')?.value);
    return new Response(response.body, { status: response.status, headers: {
      'Content-Type': response.headers.get('Content-Type') || 'application/json',
      'Cache-Control': 'no-cache',
      'X-Accel-Buffering': 'no'
    } });
  } catch (error) {
    console.error('Incident generation stream failed', error);
    return Response.json({ message: 'Не удалось подключиться к генератору' }, { status: 502 });
  }
}
