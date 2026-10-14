import { vi } from 'vitest'

export function stubJsonFetch(body: unknown, status = 200) {
  const fetchMock = vi.fn(
    async () =>
      new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
      }),
  )

  vi.stubGlobal('fetch', fetchMock)

  return fetchMock
}
