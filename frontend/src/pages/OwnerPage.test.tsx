import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import OwnerPage from './OwnerPage'

const consultation = {
  id: '1',
  name: 'Консультация',
  description: 'Разбор задачи',
  durationMinutes: 30,
}

const booking = {
  id: 'b1',
  meetingTypeId: '1',
  start: '2026-10-12T06:00:00Z',
  end: '2026-10-12T06:30:00Z',
  guestName: 'Гость',
  guestEmail: 'guest@example.com',
  comment: 'Первая встреча',
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubOwnerFetch(handlers: { types?: unknown; bookings?: unknown; created?: unknown }) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init?: RequestInit) => {
      if (init?.method === 'POST') {
        return jsonResponse(handlers.created ?? consultation, 201)
      }
      if (url.includes('/bookings')) {
        return jsonResponse(handlers.bookings ?? [])
      }
      return jsonResponse(handlers.types ?? [])
    }),
  )
}

function renderPage() {
  return render(
    <MemoryRouter>
      <OwnerPage />
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('страница владельца', () => {
  it('показывает существующие типы встреч', async () => {
    stubOwnerFetch({ types: [consultation] })
    renderPage()

    expect(await screen.findByText('Консультация')).toBeInTheDocument()
  })

  it('создаёт тип встречи и показывает его в списке', async () => {
    const user = userEvent.setup()
    let created = false

    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (init?.method === 'POST') {
          created = true
          return jsonResponse(consultation, 201)
        }
        if (url.includes('/bookings')) {
          return jsonResponse([])
        }
        return jsonResponse(created ? [consultation] : [])
      }),
    )
    renderPage()

    await user.type(screen.getByLabelText('Название'), 'Консультация')
    await user.click(screen.getByRole('button', { name: 'Создать тип встречи' }))

    expect(await screen.findByText('Консультация')).toBeInTheDocument()
  })

  it('показывает предстоящие записи с деталями', async () => {
    stubOwnerFetch({ types: [consultation], bookings: [booking] })
    renderPage()

    expect(await screen.findByText(/Гость/)).toBeInTheDocument()
    expect(screen.getByText(/guest@example\.com/)).toBeInTheDocument()
    expect(screen.getByText(/Первая встреча/)).toBeInTheDocument()
    expect(screen.getAllByText(/Консультация/).length).toBeGreaterThan(1)
  })
})
