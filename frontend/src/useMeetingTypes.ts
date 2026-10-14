import { useCallback, useEffect, useState } from 'react'
import { meetingTypesApi } from './api'
import type { MeetingType } from './generated/api'

export type MeetingTypesLoadState = 'loading' | 'ready' | 'error'

export function useMeetingTypes() {
  const [meetingTypes, setMeetingTypes] = useState<MeetingType[]>([])
  const [state, setState] = useState<MeetingTypesLoadState>('loading')

  const reload = useCallback(async () => {
    const types = await meetingTypesApi.meetingTypesList()
    setMeetingTypes(types)
    setState('ready')
  }, [])

  useEffect(() => {
    let active = true

    meetingTypesApi
      .meetingTypesList()
      .then((types) => {
        if (active) {
          setMeetingTypes(types)
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

  return { meetingTypes, state, reload }
}
