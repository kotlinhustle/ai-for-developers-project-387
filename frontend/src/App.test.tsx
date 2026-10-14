import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { stubJsonFetch } from './test/stubJsonFetch'

function LocationProbe() {
  const location = useLocation()
  return <span data-testid="location">{location.pathname}</span>
}

function renderApp(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
      <LocationProbe />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  stubJsonFetch([])
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('главная страница', () => {
  it('показывает содержимое главной', () => {
    renderApp('/')

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Записывайтесь на звонки без переписки',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Организатор открывает свободные слоты, клиент выбирает удобное время.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Записаться на звонок' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Как это работает' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Преимущества' })).toBeInTheDocument()
    expect(screen.getAllByRole('listitem')).toHaveLength(6)
    expect(screen.getByRole('contentinfo')).toHaveTextContent('Календарь звонков')
  })

  it('ведёт по CTA на страницу записи', async () => {
    const user = userEvent.setup()
    renderApp('/')

    await user.click(screen.getByRole('link', { name: 'Записаться на звонок' }))

    expect(screen.getByTestId('location')).toHaveTextContent('/booking')
    expect(screen.getByRole('heading', { level: 1, name: 'Страница записи' })).toBeInTheDocument()
    expect(await screen.findByText('Пока нет доступных типов встреч')).toBeInTheDocument()
  })

  it('показывает заглушку /booking и возвращает на главную', async () => {
    const user = userEvent.setup()
    renderApp('/booking')

    expect(screen.getByRole('heading', { level: 1, name: 'Страница записи' })).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'На главную' }))

    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Записывайтесь на звонки без переписки',
      }),
    ).toBeInTheDocument()
  })
})
