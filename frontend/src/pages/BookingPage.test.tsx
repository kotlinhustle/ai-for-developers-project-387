import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import BookingPage from './BookingPage'

const consultation = {
  id: '1',
  name: 'Консультация',
  description: 'Разбор задачи',
  durationMinutes: 30,
}

const freeSlot = { start: '2026-10-12T06:00:00Z', durationMinutes: 30 }

const booking = {
  id: 'b1',
  meetingTypeId: '1',
  start: '2026-10-12T06:00:00Z',
  end: '2026-10-12T06:30:00Z',
  guestName: 'Гость',
  guestEmail: 'guest@example.com',
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function renderPage() {
  return render(
    <MemoryRouter>
      <BookingPage />
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

async function chooseSlot(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await screen.findByRole('button', { name: /Консультация/ }))
  await user.click(await screen.findByRole('button', { name: /10:00/ }))
  await user.type(screen.getByLabelText('Имя'), 'Гость')
  await user.type(screen.getByLabelText('Email'), 'guest@example.com')
}

describe('страница записи', () => {
  it('показывает пустое состояние, когда типов встреч нет', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse([])),
    )
    renderPage()

    expect(await screen.findByText('Пока нет доступных типов встреч')).toBeInTheDocument()
  })

  it('показывает типы встреч, разобранные сгенерированным клиентом', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse([consultation])),
    )
    renderPage()

    expect(await screen.findByText('Консультация')).toBeInTheDocument()
  })

  it('показывает свободные слоты выбранного типа в поясе владельца', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string) => {
        if (url.includes('/slots')) {
          return jsonResponse({ zoneId: 'Asia/Yerevan', slots: [freeSlot] })
        }
        return jsonResponse([consultation])
      }),
    )
    renderPage()

    await user.click(await screen.findByRole('button', { name: /Консультация/ }))

    expect(await screen.findByText(/Asia\/Yerevan/)).toBeInTheDocument()
  })

  it('записывает на слот и показывает подтверждение с деталями', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (init?.method === 'POST') {
          return jsonResponse(booking, 201)
        }
        if (url.includes('/slots')) {
          return jsonResponse({ zoneId: 'Asia/Yerevan', slots: [freeSlot] })
        }
        return jsonResponse([consultation])
      }),
    )
    renderPage()

    await chooseSlot(user)
    await user.click(screen.getByRole('button', { name: 'Записаться' }))

    expect(await screen.findByText('Вы записаны')).toBeInTheDocument()
    expect(screen.getByText(/Консультация:.*30 мин/)).toBeInTheDocument()
  })

  it('показывает ошибку, когда слот уже занят', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (init?.method === 'POST') {
          return jsonResponse({ title: 'Conflict' }, 409)
        }
        if (url.includes('/slots')) {
          return jsonResponse({ zoneId: 'Asia/Yerevan', slots: [freeSlot] })
        }
        return jsonResponse([consultation])
      }),
    )
    renderPage()

    await chooseSlot(user)
    await user.click(screen.getByRole('button', { name: 'Записаться' }))

    expect(await screen.findByText('Это время уже заняли. Выберите другой слот')).toBeInTheDocument()
  })

  it('обновляет слоты после конфликта без перезагрузки страницы', async () => {
    const user = userEvent.setup()
    const nextSlot = { start: '2026-10-12T07:00:00Z', durationMinutes: 30 }
    let slotsCall = 0
    let postCall = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (init?.method === 'POST') {
          postCall += 1
          return postCall === 1
            ? jsonResponse({ title: 'Conflict' }, 409)
            : jsonResponse(booking, 201)
        }
        if (url.includes('/slots')) {
          slotsCall += 1
          return jsonResponse({
            zoneId: 'Asia/Yerevan',
            slots: slotsCall === 1 ? [freeSlot, nextSlot] : [nextSlot],
          })
        }
        return jsonResponse([consultation])
      }),
    )
    renderPage()

    await chooseSlot(user)
    await user.click(screen.getByRole('button', { name: 'Записаться' }))

    expect(await screen.findByText('Это время уже заняли. Выберите другой слот')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /11:00/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /10:00/ })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /11:00/ }))

    expect(screen.getByLabelText('Имя')).toHaveValue('Гость')
    expect(screen.getByLabelText('Email')).toHaveValue('guest@example.com')

    await user.click(screen.getByRole('button', { name: 'Записаться' }))

    expect(await screen.findByText('Вы записаны')).toBeInTheDocument()
  })

  it('убирает устаревшие слоты и сохраняет ввод, если обновление после конфликта упало', async () => {
    const user = userEvent.setup()
    const nextSlot = { start: '2026-10-12T07:00:00Z', durationMinutes: 30 }
    let slotsCall = 0
    let postCall = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (init?.method === 'POST') {
          postCall += 1
          return postCall === 1
            ? jsonResponse({ title: 'Conflict' }, 409)
            : jsonResponse(booking, 201)
        }
        if (url.includes('/slots')) {
          slotsCall += 1
          if (slotsCall === 1) {
            return jsonResponse({ zoneId: 'Asia/Yerevan', slots: [freeSlot] })
          }
          if (slotsCall === 2) {
            return jsonResponse({ title: 'Server error' }, 500)
          }
          return jsonResponse({ zoneId: 'Asia/Yerevan', slots: [nextSlot] })
        }
        return jsonResponse([consultation])
      }),
    )
    renderPage()

    await chooseSlot(user)
    await user.click(screen.getByRole('button', { name: 'Записаться' }))

    expect(await screen.findByText('Не удалось обновить свободные слоты')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /10:00/ })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Повторить загрузку' }))

    await user.click(await screen.findByRole('button', { name: /11:00/ }))

    expect(screen.getByLabelText('Имя')).toHaveValue('Гость')
    expect(screen.getByLabelText('Email')).toHaveValue('guest@example.com')

    await user.click(screen.getByRole('button', { name: 'Записаться' }))

    expect(await screen.findByText('Вы записаны')).toBeInTheDocument()
  })

  it('показывает ошибку и повторяет загрузку, если первичная загрузка слотов упала', async () => {
    const user = userEvent.setup()
    let slotsCall = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string) => {
        if (url.includes('/slots')) {
          slotsCall += 1
          return slotsCall === 1
            ? jsonResponse({ title: 'Server error' }, 500)
            : jsonResponse({ zoneId: 'Asia/Yerevan', slots: [freeSlot] })
        }
        return jsonResponse([consultation])
      }),
    )
    renderPage()

    await user.click(await screen.findByRole('button', { name: /Консультация/ }))

    expect(await screen.findByText('Не удалось обновить свободные слоты')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Повторить загрузку' }))

    expect(await screen.findByRole('button', { name: /10:00/ })).toBeInTheDocument()
  })

  it('показывает подтверждение, даже если обновление списка после записи упало', async () => {
    const user = userEvent.setup()
    let slotsCall = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (init?.method === 'POST') {
          return jsonResponse(booking, 201)
        }
        if (url.includes('/slots')) {
          slotsCall += 1
          return slotsCall === 1
            ? jsonResponse({ zoneId: 'Asia/Yerevan', slots: [freeSlot] })
            : jsonResponse({ title: 'Server error' }, 500)
        }
        return jsonResponse([consultation])
      }),
    )
    renderPage()

    await chooseSlot(user)
    await user.click(screen.getByRole('button', { name: 'Записаться' }))

    expect(await screen.findByText('Вы записаны')).toBeInTheDocument()
    expect(screen.queryByText('Не удалось обновить свободные слоты')).not.toBeInTheDocument()
  })

  it('показывает ошибку при неверных данных', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (init?.method === 'POST') {
          return jsonResponse({ title: 'Validation failed' }, 422)
        }
        if (url.includes('/slots')) {
          return jsonResponse({ zoneId: 'Asia/Yerevan', slots: [freeSlot] })
        }
        return jsonResponse([consultation])
      }),
    )
    renderPage()

    await chooseSlot(user)
    await user.click(screen.getByRole('button', { name: 'Записаться' }))

    expect(await screen.findByText('Не удалось записаться')).toBeInTheDocument()
  })
})
