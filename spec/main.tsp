import "@typespec/http";
import "@typespec/openapi";

using TypeSpec.Http;
using TypeSpec.OpenAPI;

@service(#{ title: "Календарь звонков" })
namespace Calendar;

model MeetingType {
  id: string;
  name: string;
  description: string;
  durationMinutes: int32;
}

model MeetingTypeCreate {
  @minLength(1)
  name: string;
  description: string;
  @minValue(1)
  durationMinutes: int32;
}

model Slot {
  start: utcDateTime;
  durationMinutes: int32;
}

model SlotsResponse {
  zoneId: string;
  slots: Slot[];
}

model Booking {
  id: string;
  meetingTypeId: string;
  start: utcDateTime;
  end: utcDateTime;
  guestName: string;
  guestEmail: string;
  comment?: string;
}

model BookingCreate {
  meetingTypeId: string;
  start: utcDateTime;
  @minLength(1)
  guestName: string;
  @minLength(1)
  guestEmail: string;
  comment?: string;
}

model Problem {
  title: string;
  status: int32;
  detail: string;
}

model MeetingTypeCreated {
  @statusCode _: 201;
  @body meetingType: MeetingType;
}

model BookingCreated {
  @statusCode _: 201;
  @body booking: Booking;
}

model ValidationFailed {
  @statusCode _: 422;
  @body problem: Problem;
}

model Conflict {
  @statusCode _: 409;
  @body problem: Problem;
}

@tag("MeetingTypes")
@route("/meeting-types")
interface MeetingTypes {
  @get
  list(): MeetingType[];

  @post
  create(@body payload: MeetingTypeCreate): MeetingTypeCreated | ValidationFailed;

  @get
  @route("/{meetingTypeId}/slots")
  listSlots(
    @path meetingTypeId: string,
    @query from?: plainDate,
    @query to?: plainDate,
  ): SlotsResponse;
}

@tag("Bookings")
@route("/bookings")
interface Bookings {
  @get
  list(): Booking[];

  @post
  create(@body payload: BookingCreate): BookingCreated | ValidationFailed | Conflict;
}
