import { useState, type MouseEvent } from 'react'
import type { MonthlyProduction } from '../types'

const W = 720
const H = 260
const M = { top: 14, right: 82, bottom: 30, left: 58 }
const PLOT_W = W - M.left - M.right
const PLOT_H = H - M.top - M.bottom

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const THOUSANDS = new Intl.NumberFormat('en-IN')

function periodLabel(p: MonthlyProduction): string {
  return `${MONTHS[p.month - 1]} ${String(p.year).slice(2)}`
}

function periodLabelFull(p: MonthlyProduction): string {
  return `${MONTHS[p.month - 1]} ${p.year}`
}

/** Rounds up to a clean tick max (1 / 2 / 2.5 / 5 × 10^k). */
function niceMax(value: number): number {
  if (value <= 0) return 1
  const exp = Math.floor(Math.log10(value))
  const base = Math.pow(10, exp)
  for (const m of [1, 2, 2.5, 5, 10]) {
    if (m * base >= value) return m * base
  }
  return 10 * base
}

/**
 * Two-series monthly trend (planned vs actual tonnes) as inline SVG — no chart
 * library. Fixed palette: planned = series 1, actual = series 2; the legend
 * lives in the parent card. 2px lines, >=8px end markers with a surface ring,
 * hairline gridlines, one shared axis, hover crosshair + tooltip, and a single
 * direct label on the last actual value.
 */
export default function TrendChart({ data }: { data: MonthlyProduction[] }) {
  const [hover, setHover] = useState<number | null>(null)

  if (data.length === 0) {
    return <p className="muted">No production records yet.</p>
  }

  const maxVal = Math.max(...data.map((d) => Math.max(d.plannedTonnes, d.actualTonnes)), 0)
  const yMax = niceMax(maxVal * 1.05)
  const x = (i: number) => M.left + (data.length === 1 ? PLOT_W / 2 : (i / (data.length - 1)) * PLOT_W)
  const y = (v: number) => M.top + PLOT_H - (v / yMax) * PLOT_H

  const line = (key: 'plannedTonnes' | 'actualTonnes') =>
    data.map((d, i) => `${x(i)},${y(d[key])}`).join(' ')

  const ticks = [0, 0.25, 0.5, 0.75, 1].map((f) => f * yMax)

  // Tooltip geometry (kept inside the viewBox).
  const active = hover === null ? data.length - 1 : hover
  const tipW = 150
  const tipH = 58
  const tipX = Math.min(Math.max(x(active) - tipW / 2, M.left), W - M.right - tipW)
  const tipY = M.top + 6
  const crossX = x(active)

  function onMove(event: MouseEvent<SVGSVGElement>) {
    const rect = event.currentTarget.getBoundingClientRect()
    const svgX = ((event.clientX - rect.left) / rect.width) * W
    let nearest = 0
    let best = Infinity
    for (let i = 0; i < data.length; i++) {
      const d = Math.abs(x(i) - svgX)
      if (d < best) {
        best = d
        nearest = i
      }
    }
    setHover(nearest)
  }

  const point = data[active]

  return (
    <svg
      className="trend-svg"
      viewBox={`0 0 ${W} ${H}`}
      role="img"
      aria-label={`Monthly production versus plan, ${periodLabelFull(data[0])} to ${periodLabelFull(data[data.length - 1])}`}
      tabIndex={0}
      onMouseMove={onMove}
      onMouseLeave={() => setHover(null)}
      onFocus={() => setHover(data.length - 1)}
      onBlur={() => setHover(null)}
    >
      {/* hairline gridlines + baseline */}
      {ticks.map((t, i) => (
        <g key={i}>
          <line className="gridline" x1={M.left} x2={W - M.right} y1={y(t)} y2={y(t)} />
          <text x={M.left - 8} y={y(t) + 4} textAnchor="end">
            {i === 0 ? '0' : THOUSANDS.format(t)}
          </text>
        </g>
      ))}
      <line className="axis-line" x1={M.left} x2={W - M.right} y1={M.top + PLOT_H} y2={M.top + PLOT_H} />

      {/* x labels, every other period */}
      {data.map((d, i) =>
        i % 2 === 0 || i === data.length - 1 ? (
          <text key={i} x={x(i)} y={H - 8} textAnchor="middle">
            {periodLabel(d)}
          </text>
        ) : null,
      )}

      {/* the two series — color follows the entity, never the rank */}
      <polyline className="series-1" points={line('plannedTonnes')} />
      <polyline className="series-2" points={line('actualTonnes')} />

      {/* end markers: filled dot with a surface ring */}
      <circle cx={x(data.length - 1)} cy={y(data[data.length - 1].plannedTonnes)} r={4} fill="var(--series-1)" stroke="var(--surface)" strokeWidth={2} />
      <circle cx={x(data.length - 1)} cy={y(data[data.length - 1].actualTonnes)} r={4} fill="var(--series-2)" stroke="var(--surface)" strokeWidth={2} />

      {/* single direct label: the last actual value only */}
      <text x={x(data.length - 1) + 10} y={y(data[data.length - 1].actualTonnes) + 4} style={{ fill: 'var(--ink-2)', fontWeight: 600 }}>
        {THOUSANDS.format(Math.round(data[data.length - 1].actualTonnes))} t
      </text>

      {/* hover crosshair + tooltip */}
      <g>
        <line x1={crossX} x2={crossX} y1={M.top} y2={M.top + PLOT_H} stroke="var(--baseline)" strokeWidth={1} />
        <circle cx={crossX} cy={y(point.plannedTonnes)} r={4} fill="var(--series-1)" stroke="var(--surface)" strokeWidth={2} />
        <circle cx={crossX} cy={y(point.actualTonnes)} r={4} fill="var(--series-2)" stroke="var(--surface)" strokeWidth={2} />
        <rect className="tooltip" x={tipX} y={tipY} width={tipW} height={tipH} rx={4} strokeWidth={1} />
        <text x={tipX + 10} y={tipY + 18} style={{ fill: 'var(--ink)', fontWeight: 600 }}>
          {periodLabelFull(point)}
        </text>
        <rect x={tipX + 10} y={tipY + 27} width={12} height={3} rx={1.5} fill="var(--series-1)" />
        <text x={tipX + 28} y={tipY + 32} style={{ fill: 'var(--ink-2)' }}>
          Planned {THOUSANDS.format(Math.round(point.plannedTonnes))} t
        </text>
        <rect x={tipX + 10} y={tipY + 43} width={12} height={3} rx={1.5} fill="var(--series-2)" />
        <text x={tipX + 28} y={tipY + 48} style={{ fill: 'var(--ink-2)' }}>
          Actual {THOUSANDS.format(Math.round(point.actualTonnes))} t
        </text>
      </g>
    </svg>
  )
}
