import React, { useCallback, useEffect, useMemo, useState } from 'react'

const STATUS_OPTIONS = {
  configuration_requested: ['under_review', 'rejected'],
  under_review: ['approved', 'rejected'],
  approved: ['provisioning', 'rejected'],
  provisioning: ['active', 'rejected'],
  active: ['suspended', 'closed'],
  suspended: ['active', 'closed'],
  rejected: ['closed'],
  closed: [],
}
const label = (value) => String(value || 'unknown').replaceAll('_', ' ')
const fmtDate = (value) => value ? new Date(value).toLocaleString('en-KE', { dateStyle: 'medium', timeStyle: 'short' }) : '—'

export default function EnterpriseRequests({ supabase, session, onBack, onSignOut }) {
  const [requests, setRequests] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [filter, setFilter] = useState('open')
  const [targetStatus, setTargetStatus] = useState('')
  const [reason, setReason] = useState('')
  const [notes, setNotes] = useState('')
  const [organisationId, setOrganisationId] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const callAdmin = useCallback(async (body) => {
    const { data, error: invokeError } = await supabase.functions.invoke('review-enterprise-request', { body })
    if (invokeError) throw new Error(invokeError.message || 'Enterprise request operation failed.')
    if (data?.error) throw new Error(data.message || data.error)
    return data
  }, [supabase])

  const load = useCallback(async () => {
    setBusy(true); setError('')
    try {
      const data = await callAdmin({ action: 'list' })
      setRequests(data.requests || [])
      setSelectedId((current) => current && (data.requests || []).some((r) => r.id === current) ? current : (data.requests || [])[0]?.id || '')
    } catch (e) { setError(e.message || 'Could not load enterprise requests.') }
    finally { setBusy(false) }
  }, [callAdmin])

  useEffect(() => { load() }, [load])
  const visible = useMemo(() => requests.filter((r) => filter === 'all' || (filter === 'open' ? !['active','rejected','closed'].includes(r.status) : r.status === filter)), [requests, filter])
  const selected = requests.find((r) => r.id === selectedId) || null
  const allowedNext = selected ? (STATUS_OPTIONS[selected.status] || []) : []

  useEffect(() => {
    setTargetStatus(allowedNext[0] || '')
    setReason('')
    setNotes(selected?.review_notes || '')
    setOrganisationId(selected?.provisioned_organisation_id || '')
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId, selected?.status])

  async function transition(event) {
    event.preventDefault()
    if (!selected || !targetStatus) return
    if (!window.confirm(`Change request status from "${label(selected.status)}" to "${label(targetStatus)}"?`)) return
    setBusy(true); setError(''); setNotice('')
    try {
      const data = await callAdmin({
        request_id: selected.id,
        to_status: targetStatus,
        reason: reason.trim(),
        review_notes: notes.trim() || null,
        provisioned_organisation_id: organisationId.trim() || null,
      })
      setNotice(`Request updated to ${label(data.request?.status || targetStatus)}.`)
      await load()
    } catch (e) { setError(e.message || 'Request could not be updated.') }
    finally { setBusy(false) }
  }

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark small">M</div><div><strong>MobiWave CNC</strong><span>Platform administration</span></div></div>
      <div className="side-label">ADMINISTRATION</div>
      <button className="nav" onClick={onBack}><span className="nav-icon">▦</span>Client organisations</button>
      <button className="nav active"><span className="nav-icon">⇥</span>Enterprise requests</button>
      <button className="nav" onClick={() => { window.location.hash = 'billing' }}><span className="nav-icon">▤</span>Billing & subscriptions</button>
      <button className="nav" onClick={() => { window.location.hash = 'plans' }}><span className="nav-icon">◇</span>Plans & limits</button>
      <div className="sidebar-bottom"><div className="user-chip"><div className="avatar">{(session?.user?.email || 'A').slice(0,1).toUpperCase()}</div><div className="user-meta"><strong>{session?.user?.email}</strong><span>Platform administrator</span></div></div><button className="signout" onClick={onSignOut}>Sign out <span>↗</span></button></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><div><p className="eyebrow">MOBIWAVE CNC / ADMISSION</p><h1>Enterprise requests</h1><p className="muted">Review configuration requests. Approval does not automatically create or activate a tenant.</p></div><button className="secondary" onClick={load} disabled={busy}>{busy ? 'Refreshing…' : '↻ Refresh requests'}</button></header>
      {error && <div className="notice" role="alert"><strong>Action could not be completed</strong><span>{error}</span><button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}
      {notice && <div className="success-notice" role="status"><strong>Updated</strong><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      <section className="stat-grid">
        <article className="stat"><span className="stat-label">Total requests</span><strong>{requests.length}</strong><span className="stat-note">All submitted requests</span></article>
        <article className="stat"><span className="stat-label">Awaiting review</span><strong>{requests.filter(r => ['configuration_requested','under_review'].includes(r.status)).length}</strong><span className="stat-note">Needs an admission decision</span></article>
        <article className="stat"><span className="stat-label">In provisioning</span><strong>{requests.filter(r => ['approved','provisioning'].includes(r.status)).length}</strong><span className="stat-note">Tenant setup is still explicit</span></article>
        <article className="stat"><span className="stat-label">Active</span><strong>{requests.filter(r => r.status === 'active').length}</strong><span className="stat-note">Linked to an existing organisation</span></article>
      </section>
      <section className="table-panel">
        <div className="panel-head"><div><h2>Admission queue</h2><p className="muted">Request records are read through the trusted server function, not direct client table access.</p></div><span className="record-count">{visible.length} shown</span></div>
        <div className="table-tools"><select value={filter} onChange={e => setFilter(e.target.value)} aria-label="Filter requests"><option value="open">Open requests</option><option value="all">All requests</option>{Object.keys(STATUS_OPTIONS).map(s => <option key={s} value={s}>{label(s)}</option>)}</select></div>
        <div className="table-scroll"><table><thead><tr><th>Company</th><th>Contact</th><th>Users</th><th>Status</th><th>Submitted</th></tr></thead><tbody>{visible.map(r => <tr key={r.id} className={selectedId === r.id ? 'selected-row' : ''} onClick={() => setSelectedId(r.id)} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); setSelectedId(r.id) } }} tabIndex={0} aria-selected={selectedId === r.id} style={{cursor:'pointer'}}><td><strong>{r.legal_name}</strong><small>{r.trading_name || r.country_code || '—'}</small></td><td>{r.contact_name}<small>{r.contact_email}</small></td><td>{r.estimated_users ?? 'Not specified'}</td><td><span className={'pill ' + (r.status === 'active' ? 'good' : ['rejected','closed','suspended'].includes(r.status) ? 'bad' : 'pending')}>{label(r.status)}</span></td><td>{fmtDate(r.created_at)}</td></tr>)}</tbody></table>{!visible.length && <div className="empty"><strong>{busy ? 'Loading requests…' : 'No requests in this view'}</strong><p>New enterprise requests will appear here after submission.</p></div>}</div>
      </section>
      {selected && <section className="table-panel">
        <div className="panel-head"><div><h2>{selected.legal_name}</h2><p className="muted">Request reference {selected.id}</p></div><span className={'pill ' + (selected.status === 'active' ? 'good' : 'pending')}>{label(selected.status)}</span></div>
        <div className="detail-grid">
          <div><span className="stat-label">Trading name</span><strong>{selected.trading_name || '—'}</strong></div>
          <div><span className="stat-label">Primary contact</span><strong>{selected.contact_name}</strong></div>
          <div><span className="stat-label">Email / phone</span><strong>{selected.contact_email}<br/>{selected.contact_phone || '—'}</strong></div>
          <div><span className="stat-label">Country / estimated users</span><strong>{selected.country_code} / {selected.estimated_users ?? 'Not specified'}</strong></div>
          <div><span className="stat-label">Locations</span><p>{selected.locations_summary || 'Not provided'}</p></div>
          <div><span className="stat-label">Departments</span><p>{selected.department_summary || 'Not provided'}</p></div>
          <div className="detail-wide"><span className="stat-label">Configuration requirements</span><p>{selected.configuration_requirements || 'Not provided'}</p></div>
        </div>
        {allowedNext.length > 0 && <form className="admin-create-form transition-form" onSubmit={transition}>
          <label>Next status<select required value={targetStatus} onChange={e => setTargetStatus(e.target.value)}>{allowedNext.map(s => <option key={s} value={s}>{label(s)}</option>)}</select></label>
          <label>Reason for audit trail<textarea required minLength={3} maxLength={4000} value={reason} onChange={e => setReason(e.target.value)} placeholder="Explain this decision or workflow transition." /></label>
          <label>Internal review notes<textarea maxLength={20000} value={notes} onChange={e => setNotes(e.target.value)} placeholder="Optional internal notes for the next reviewer." /></label>
          {targetStatus === 'active' && <label>Existing organisation UUID (required to activate)<input required value={organisationId} onChange={e => setOrganisationId(e.target.value)} placeholder="UUID of the provisioned organisation" /></label>}
          <button className="primary" disabled={busy || reason.trim().length < 3}>{busy ? 'Saving…' : 'Record audited transition'}</button>
        </form>}
        {allowedNext.length === 0 && <p className="muted">This request is closed. No further transitions are available.</p>}
      </section>}
      <footer className="app-footer">MobiWave Innovations Ltd <span>·</span> Enterprise admission <span>·</span> Audited status changes</footer>
    </main>
  </div>
}
