/**
 * Locale-independent date formatting utility matching DateKeys.kt
 * Formats dates consistently as YYYY-MM-DD with standard US digits.
 */

export function getTodayKey(): string {
  return formatDateKey(new Date());
}

export function formatDateKey(d: Date): string {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatDayDisplay(d: Date): string {
  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  return d.toLocaleDateString('en-US', options);
}

export function getDayOffsetKey(offsetDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return formatDateKey(d);
}

export function parseDateKey(key: string): Date {
  const [year, month, day] = key.split('-').map(Number);
  return new Date(year, (month || 1) - 1, day || 1);
}
