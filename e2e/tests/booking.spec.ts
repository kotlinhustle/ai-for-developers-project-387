import { expect, test } from '@playwright/test'

test('владелец создаёт тип, гость бронирует слот, владелец видит запись', async ({ page, request }) => {
  const runId = Date.now()
  const typeName = `Консультация ${runId}`
  const guestEmail = `guest-${runId}@example.com`

  // Owner creates a meeting type
  await page.goto('/owner')
  await page.getByLabel('Название').fill(typeName)
  await page.getByLabel('Описание').fill('Разбор задачи')
  await page.getByLabel('Длительность, мин').fill('30')

  const [createResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/meeting-types') && response.request().method() === 'POST',
    ),
    page.getByRole('button', { name: 'Создать тип встречи' }).click(),
  ])
  expect(createResponse.status()).toBe(201)
  const meetingType = (await createResponse.json()) as { id: string }
  // Derive the API prefix from the SDK's own request URL ('' when the backend
  // serves the frontend, '/api' behind the Vite dev proxy).
  const apiPrefix = new URL(createResponse.url()).pathname.replace(/\/meeting-types$/, '')

  // Guest sees the type and its free slots
  await page.goto('/booking')
  const [slotsResponse] = await Promise.all([
    page.waitForResponse((response) => response.url().includes('/slots') && response.status() === 200),
    page.getByRole('button', { name: new RegExp(typeName) }).click(),
  ])
  const slots = (await slotsResponse.json()).slots as Array<{ start: string }>
  expect(slots.length).toBeGreaterThan(0)
  const bookedStart = slots[0].start

  // Guest books the first free slot
  await page.locator('.slots button').first().click()
  await page.getByLabel('Имя').fill('Гость')
  await page.getByLabel('Email').fill(guestEmail)
  await page.getByRole('button', { name: 'Записаться' }).click()
  await expect(page.getByText('Вы записаны')).toBeVisible()

  // A second booking for the same slot is rejected with 409
  const conflict = await request.post(`${apiPrefix}/bookings`, {
    data: {
      meetingTypeId: meetingType.id,
      start: bookedStart,
      guestName: 'Второй гость',
      guestEmail: 'second@example.com',
    },
  })
  expect(conflict.status()).toBe(409)

  // The booked slot is no longer offered as free
  await page.goto('/booking')
  const [slotsAgain] = await Promise.all([
    page.waitForResponse((response) => response.url().includes('/slots') && response.status() === 200),
    page.getByRole('button', { name: new RegExp(typeName) }).click(),
  ])
  const remaining = (await slotsAgain.json()).slots as Array<{ start: string }>
  expect(remaining.map((slot) => slot.start)).not.toContain(bookedStart)

  // Owner sees the booking in the upcoming list
  await page.goto('/owner')
  await expect(page.getByText(guestEmail)).toBeVisible()
})
