import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { bookingsApi, meetingTypesApi } from '../api'
import { formatSlot } from '../format'
import { ResponseError } from '../generated/api'
import type { Booking, MeetingType, Slot, SlotsResponse } from '../generated/api'
import { useMeetingTypes } from '../useMeetingTypes'

function BookingPage() {
  const { meetingTypes, state } = useMeetingTypes()
  const [selected, setSelected] = useState<MeetingType | null>(null)
  const [slots, setSlots] = useState<SlotsResponse | null>(null)
  const [chosenSlot, setChosenSlot] = useState<Slot | null>(null)
  const [guestName, setGuestName] = useState('')
  const [guestEmail, setGuestEmail] = useState('')
  const [comment, setComment] = useState('')
  const [confirmation, setConfirmation] = useState<Booking | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [slotsError, setSlotsError] = useState<string | null>(null)

  function fetchSlots(meetingType: MeetingType) {
    return meetingTypesApi.meetingTypesListSlots({ meetingTypeId: meetingType.id })
  }

  async function loadSlots(meetingType: MeetingType) {
    try {
      setSlots(await fetchSlots(meetingType))
      setSlotsError(null)
    } catch {
      setSlots(null)
      setSlotsError('Не удалось обновить свободные слоты')
    }
  }

  async function refreshSlotsQuietly(meetingType: MeetingType) {
    try {
      setSlots(await fetchSlots(meetingType))
      setSlotsError(null)
    } catch {
      setSlotsError(null)
    }
  }

  async function selectType(meetingType: MeetingType) {
    setSelected(meetingType)
    setSlots(null)
    setChosenSlot(null)
    setConfirmation(null)
    setError(null)
    setSlotsError(null)

    await loadSlots(meetingType)
  }

  async function book(event: FormEvent) {
    event.preventDefault()
    if (!selected || !chosenSlot) {
      return
    }
    setError(null)

    try {
      const booking = await bookingsApi.bookingsCreate({
        bookingCreate: {
          meetingTypeId: selected.id,
          start: chosenSlot.start,
          guestName,
          guestEmail,
          comment: comment || undefined,
        },
      })
      setConfirmation(booking)
      setChosenSlot(null)
      setGuestName('')
      setGuestEmail('')
      setComment('')
    } catch (err) {
      if (err instanceof ResponseError && err.response.status === 409) {
        setError('Это время уже заняли. Выберите другой слот')
        setChosenSlot(null)
        await loadSlots(selected)
      } else {
        setError('Не удалось записаться')
      }
      return
    }

    await refreshSlotsQuietly(selected)
  }

  return (
    <main className="page">
      <h1 className="hero__title">Страница записи</h1>

      {state === 'loading' && <p className="status status--loading">Загрузка типов встреч…</p>}
      {state === 'error' && <p className="status status--down">Не удалось загрузить типы встреч</p>}
      {state === 'ready' && meetingTypes.length === 0 && (
        <p className="status status--loading">Пока нет доступных типов встреч</p>
      )}
      {state === 'ready' && meetingTypes.length > 0 && (
        <ul className="benefits">
          {meetingTypes.map((meetingType) => (
            <li key={meetingType.id} className="benefits__item">
              <button
                className="button button--secondary"
                type="button"
                onClick={() => selectType(meetingType)}
              >
                <strong>{meetingType.name}</strong> — {meetingType.durationMinutes} мин
              </button>
            </li>
          ))}
        </ul>
      )}

      {error && <p className="status status--down">{error}</p>}

      {slotsError && (
        <p className="status status--down">
          {slotsError}{' '}
          <button
            className="button button--secondary"
            type="button"
            onClick={() => {
              if (selected) {
                void loadSlots(selected)
              }
            }}
          >
            Повторить загрузку
          </button>
        </p>
      )}

      {selected && slots && (
        <section className="section" aria-labelledby="slots-title">
          <h2 id="slots-title" className="section__title">
            {selected.name}: свободные слоты
          </h2>

          {slots.slots.length === 0 && <p className="status status--loading">Нет свободных слотов</p>}
          {slots.slots.length > 0 && (
            <>
              <p className="hero__subtitle">Время в поясе {slots.zoneId}</p>
              <ul className="slots">
                {slots.slots.map((slot) => (
                  <li key={slot.start.toISOString()}>
                    <button
                      className="slots__item"
                      type="button"
                      onClick={() => {
                        setChosenSlot(slot)
                        setConfirmation(null)
                        setError(null)
                      }}
                    >
                      {formatSlot(slot.start, slots.zoneId)}
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      )}

      {selected && chosenSlot && (
        <section className="section" aria-labelledby="book-title">
          <h2 id="book-title" className="section__title">
            Запись: {formatSlot(chosenSlot.start, slots?.zoneId ?? 'UTC')}
          </h2>
          <form className="form" onSubmit={book}>
            <label className="form__field">
              Имя
              <input
                className="form__input"
                value={guestName}
                onChange={(event) => setGuestName(event.target.value)}
                required
              />
            </label>
            <label className="form__field">
              Email
              <input
                className="form__input"
                type="email"
                value={guestEmail}
                onChange={(event) => setGuestEmail(event.target.value)}
                required
              />
            </label>
            <label className="form__field">
              Комментарий
              <input
                className="form__input"
                value={comment}
                onChange={(event) => setComment(event.target.value)}
              />
            </label>
            <button className="button" type="submit">
              Записаться
            </button>
          </form>
        </section>
      )}

      {confirmation && selected && (
        <section className="section">
          <h2 className="section__title">Вы записаны</h2>
          <p className="status status--up">
            {selected.name}: {formatSlot(confirmation.start, slots?.zoneId ?? 'UTC')} (
            {selected.durationMinutes} мин)
          </p>
        </section>
      )}

      <Link className="button button--secondary" to="/">
        На главную
      </Link>
    </main>
  )
}

export default BookingPage
