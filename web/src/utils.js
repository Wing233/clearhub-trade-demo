export const num = (value) => {
  if (value === null || value === undefined || value === '') return 0
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

export const fmt = (value, digits = 2) =>
  num(value).toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })

export const fmtQty = (value) => {
  const parsed = num(value)
  if (Number.isInteger(parsed)) {
    return parsed.toLocaleString('zh-CN')
  }
  return fmt(parsed, 2)
}

export const changeClass = (value) => {
  const parsed = num(value)
  if (parsed > 0) return 'up'
  if (parsed < 0) return 'down'
  return 'flat'
}

export const signText = (value) => {
  const parsed = num(value)
  if (parsed > 0) return `+${fmt(parsed)}`
  return fmt(parsed)
}

export const timeText = (value) => {
  if (!value) return '--'
  const text = String(value).replace('T', ' ')
  if (text.length <= 8) return text
  return text.slice(11, 19)
}

export const dateTimeText = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 19)
}

export const tickPrecision = (tick) => {
  const text = String(tick ?? '0.01')
  const index = text.indexOf('.')
  return index < 0 ? 0 : text.length - index - 1
}
