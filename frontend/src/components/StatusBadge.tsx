type Tone = 'good' | 'warning' | 'critical' | 'neutral'

const TONES: Record<string, Tone> = {
  APPROVED: 'good',
  ACTIVE: 'good',
  enabled: 'good',
  DRAFT: 'warning',
  RETIRED: 'neutral',
  TRANSFERRED: 'neutral',
  REJECTED: 'critical',
}

/**
 * Status is always icon/shape + text label, never color alone
 * (sub-3:1 status hues on the light surface rely on the label).
 */
export default function StatusBadge({ status }: { status: string | boolean }) {
  const key = typeof status === 'boolean' ? (status ? 'enabled' : 'REJECTED') : status
  const tone = TONES[key] ?? 'neutral'
  const label = typeof status === 'boolean' ? (status ? 'Active' : 'Disabled') : key.charAt(0) + key.slice(1).toLowerCase()
  return (
    <span className={`badge badge-${tone}`}>
      <span className="badge-dot" aria-hidden="true" />
      {label}
    </span>
  )
}
