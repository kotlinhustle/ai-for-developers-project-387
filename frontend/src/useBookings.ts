import { useCallback, useEffect, useState } from 'react'
import { bookingsApi } from './api'
import type { Booking } from './generated/api'

export type BookingsLoadState = 'loading' | 'ready' | 'error'

export function useBookings() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [state, setState] = useState<BookingsLoadState>('loading')

  const reload = useCallback(async () => {
    setBookings(await bookingsApi.bookingsList())
    setState('ready')
  }, [])

  useEffect(() => {
    let active = true

    bookingsApi
      .bookingsList()
      .then((list) => {
        if (active) {
          setBookings(list)
          setState('ready')
        }
      })
      .catch(() => {
        if (active) {
          setState('error')
        }
      })

    return () => {
      active = false
    }
  }, [])

  return { bookings, state, reload }
}
