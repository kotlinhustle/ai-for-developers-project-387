import { BookingsApi, Configuration, MeetingTypesApi } from './generated/api'

const basePath = import.meta.env.VITE_API_BASE_PATH ?? '/api'
const configuration = new Configuration({ basePath })

export const meetingTypesApi = new MeetingTypesApi(configuration)
export const bookingsApi = new BookingsApi(configuration)
