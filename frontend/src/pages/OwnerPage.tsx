import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { meetingTypesApi } from '../api'
import { formatSlot } from '../format'
import { ResponseError } from '../generated/api'
import { useBookings } from '../useBookings'
import { useMeetingTypes } from '../useMeetingTypes'

const OWNER_ZONE = 'Asia/Yerevan'

function OwnerPage() {
  const { meetingTypes, state, reload } = useMeetingTypes()
  const { bookings, state: bookingsState } = useBookings()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [durationMinutes, setDurationMinutes] = useState(30)
  const [createError, setCreateError] = useState<string | null>(null)

  function meetingTypeName(meetingTypeId: string) {
    return meetingTypes.find((meetingType) => meetingType.id === meetingTypeId)?.name ?? 'Тип встречи'
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setCreateError(null)

    try {
      await meetingTypesApi.meetingTypesCreate({
        meetingTypeCreate: { name, description, durationMinutes },
      })
      setName('')
      setDescription('')
      setDurationMinutes(30)
      await reload()
    } catch (error) {
      setCreateError(
        error instanceof ResponseError && error.response.status === 422
          ? 'Проверьте название и длительность'
          : 'Не удалось создать тип встречи',
      )
    }
  }

  return (
    <main className="page">
      <h1 className="hero__title">Типы встреч</h1>

      <form className="form" onSubmit={handleSubmit}>
        <label className="form__field">
          Название
          <input
            className="form__input"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </label>
        <label className="form__field">
          Описание
          <input
            className="form__input"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </label>
        <label className="form__field">
          Длительность, мин
          <input
            className="form__input"
            type="number"
            min={1}
            value={durationMinutes}
            onChange={(event) => setDurationMinutes(Number(event.target.value))}
            required
          />
        </label>
        <button className="button" type="submit">
          Создать тип встречи
        </button>
      </form>

      {state === 'error' && <p className="status status--down">Не удалось загрузить типы встреч</p>}
      {createError && <p className="status status--down">{createError}</p>}

      <ul className="benefits">
        {meetingTypes.map((meetingType) => (
          <li key={meetingType.id} className="benefits__item">
            <strong>{meetingType.name}</strong> — {meetingType.durationMinutes} мин
          </li>
        ))}
      </ul>

      <section className="section" aria-labelledby="bookings-title">
        <h2 id="bookings-title" className="section__title">
          Предстоящие записи
        </h2>
        {bookingsState === 'loading' && <p className="status status--loading">Загрузка записей…</p>}
        {bookingsState === 'error' && (
          <p className="status status--down">Не удалось загрузить записи</p>
        )}
        {bookingsState === 'ready' && bookings.length === 0 && (
          <p className="status status--loading">Пока нет записей</p>
        )}
        {bookingsState === 'ready' && bookings.length > 0 && (
          <ul className="benefits">
            {bookings.map((booking) => (
              <li key={booking.id} className="benefits__item">
                <strong>{meetingTypeName(booking.meetingTypeId)}</strong>:{' '}
                {formatSlot(booking.start, OWNER_ZONE)} — {booking.guestName} ({booking.guestEmail})
                {booking.comment ? ` — ${booking.comment}` : ''}
              </li>
            ))}
          </ul>
        )}
      </section>

      <Link className="button button--secondary" to="/">
        На главную
      </Link>
    </main>
  )
}

export default OwnerPage
