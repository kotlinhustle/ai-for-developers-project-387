export function formatSlot(start: Date, zoneId: string) {
  return new Intl.DateTimeFormat('ru-RU', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: zoneId,
  }).format(start)
}
