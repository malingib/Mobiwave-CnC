import React, { useCallback, useEffect, useMemo, useState } from 'react'

const TABS = [
  { id: 'units', label: 'Units & departments' },
  { id: 'employees', label: 'Employees & import' },
  { id: 'roles', label: 'Roles' },
  { id: 'assignments', label: 'Assignments' },
  { id: 'reporting', label: 'Reporting lines' },
]
const PERMISSIONS = [
  ['organisation.manage', 'Manage organisation structure'],
  ['employees.manage', 'Manage employee records'],
  ['roles.manage', 'Manage roles and permissions'],
  ['allocations.manage', 'Manage expense allocations'],
  ['disbursements.prepare', 'Prepare disbursement batches'],
  ['disbursements.approve', 'Approve disbursement batches'],
  ['disbursements.submit', 'Submit approved batches'],
  ['expenses.review', 'Review expenses'],
  ['reconciliations.manage', 'Manage reconciliations'],
  ['audit.read', 'Read audit events'],
]
function parseCsvLine(line) {
  const cells = []; let value = ''; let quoted = false
  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    if (char === '"' && quoted && line[i + 1] === '"') { value += '"'; i++ }
    else if (char === '"') quoted = !quoted
    else if (char === ',' && !quoted) { cells.push(value.trim()); value = '' }
    else value += char
  }
  if (quoted) throw new Error('CSV contains an unclosed quoted field.')
  cells.push(value.trim())
  return cells
}
function parseEmployeeCsv(text) {
  const lines = text.replace(/^\uFEFF/, '').split(/\r?\n/).filter(line => line.trim())
  if (lines.length < 2) throw new Error('CSV needs a header and at least one employee row.')
  const headers = parseCsvLine(lines[0]).map(x => x.toLowerCase())
  const required = ['employee_number', 'full_name']
  const missing = required.filter(x => !headers.includes(x))
  if (missing.length) throw new Error('Missing required CSV columns: ' + missing.join(', '))
  const rows = lines.slice(1).map((line, i) => {
    const cells = parseCsvLine(line)
    const row = Object.fromEntries(headers.map((h, j) => [h, cells[j] || '']))
    return { line: i + 2, employee_number: row.employee_number.trim(), full_name: row.full_name.trim(), email: row.email.trim() || null, phone_e164: row.phone_e164.trim() || null, status: (row.status || 'active').toLowerCase() }
  })
  const errors = []
  const seen = new Set()
  rows.forEach(r => {
    if (!r.employee_number || !r.full_name) errors.push('Line ' + r.line + ': employee_number and full_name are required.')
    if (!['active','inactive','suspended'].includes(r.status)) errors.push('Line ' + r.line + ': status must be active, inactive or suspended.')
    if (seen.has(r.employee_number)) errors.push('Line ' + r.line + ': duplicate employee_number ' + r.employee_number + ' in this file.')
    seen.add(r.employee_number)
  })
  return { rows, errors }
}
const fmtDate = (v) => v ? new Date(v).toLocaleDateString('en-KE', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'

export default function OrganisationConfiguration({ supabase, session, onBack, onSignOut }) {
  const [organisationId, setOrganisationId] = useState('')
  const [tab, setTab] = useState('units')
  const [units, setUnits] = useState([])
  const [roles, setRoles] = useState([])
  const [employees, setEmployees] = useState([])
  const [assignments, setAssignments] = useState([])
  const [reporting, setReporting] = useState([])
  const [csvRows, setCsvRows] = useState([])
  const [csvErrors, setCsvErrors] = useState([])
  const [csvFileName, setCsvFileName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [unit, setUnit] = useState({ unit_type: 'shop', code: '', name: '', parent_id: '' })
  const [role, setRole] = useState({ role_key: '', name: '', description: '', permissions: [] })
  const [assignment, setAssignment] = useState({ employee_id: '', role_id: '', org_unit_id: '', starts_at: new Date().toISOString().slice(0,10), ends_at: '', is_acting: false })
  const [line, setLine] = useState({ employee_id: '', manager_employee_id: '', org_unit_id: '', starts_at: new Date().toISOString().slice(0,10), ends_at: '' })

  const load = useCallback(async () => {
    setBusy(true); setError('')
    try {
      const { data: orgId, error: orgError } = await supabase.rpc('current_organisation_id')
      if (orgError) throw orgError
      if (!orgId) throw new Error('Your account is not linked to an organisation workspace.')
      setOrganisationId(orgId)
      const [u, r, e, a, rr] = await Promise.all([
        supabase.from('org_units').select('id,parent_id,unit_type,code,name,status,metadata,created_at').order('unit_type').order('name'),
        supabase.from('roles').select('id,role_key,name,description,is_system_role,created_at').order('name'),
        supabase.from('employees').select('id,employee_number,full_name,email,status').order('full_name'),
        supabase.from('employee_assignments').select('id,employee_id,role_id,org_unit_id,starts_at,ends_at,is_acting,acting_for_assignment_id,status').order('starts_at', { ascending: false }).limit(500),
        supabase.from('reporting_relationships').select('id,employee_id,manager_employee_id,org_unit_id,starts_at,ends_at').order('starts_at', { ascending: false }).limit(500),
      ])
      const failed = [u,r,e,a,rr].find(x => x.error)
      if (failed?.error) throw failed.error
      setUnits(u.data || []); setRoles(r.data || []); setEmployees(e.data || [])
      setAssignments(a.data || []); setReporting(rr.data || [])
      setAssignment(v => ({ ...v, employee_id: v.employee_id || e.data?.[0]?.id || '', role_id: v.role_id || r.data?.[0]?.id || '', org_unit_id: v.org_unit_id || u.data?.[0]?.id || '' }))
      setLine(v => ({ ...v, employee_id: v.employee_id || e.data?.[0]?.id || '', manager_employee_id: v.manager_employee_id || e.data?.find(x => x.id !== (v.employee_id || e.data?.[0]?.id))?.id || '', org_unit_id: v.org_unit_id || u.data?.[0]?.id || '' }))
    } catch (e) { setError(e.message || 'Could not load organisation configuration.') }
    finally { setBusy(false) }
  }, [supabase])

  useEffect(() => { load() }, [load])
  const unitNames = useMemo(() => Object.fromEntries(units.map(x => [x.id, x.name])), [units])
  const roleNames = useMemo(() => Object.fromEntries(roles.map(x => [x.id, x.name])), [roles])
  const employeeNames = useMemo(() => Object.fromEntries(employees.map(x => [x.id, x.full_name])), [employees])

  async function save(event, table, values, reset) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      const { error: insertError } = await supabase.from(table).insert({ organisation_id: organisationId, ...values })
      if (insertError) throw insertError
      setNotice('Configuration saved.')
      if (reset) reset()
      await load()
    } catch (e) { setError(e.message || 'Could not save configuration. Check your organisation-management permissions and field values.') }
    finally { setBusy(false) }
  }

  async function readEmployeeCsv(event) {
    const file = event.target.files?.[0]
    if (!file) return
    setCsvFileName(file.name); setCsvRows([]); setCsvErrors([]); setError('')
    try {
      if (file.size > 5 * 1024 * 1024) throw new Error('CSV file must be 5 MB or smaller.')
      const parsed = parseEmployeeCsv(await file.text())
      if (parsed.rows.length > 2000) throw new Error('Import is limited to 2,000 employee rows per batch.')
      setCsvRows(parsed.rows); setCsvErrors(parsed.errors)
    } catch (e) { setCsvErrors([e.message || 'Could not parse CSV.']) }
  }

  async function importEmployees() {
    if (!organisationId || !csvRows.length || csvErrors.length) return
    setBusy(true); setError(''); setNotice('')
    try {
      const numbers = csvRows.map(r => r.employee_number)
      const existing = []
      for (let i = 0; i < numbers.length; i += 100) {
        const { data, error: existingError } = await supabase.from('employees').select('employee_number').in('employee_number', numbers.slice(i, i + 100))
        if (existingError) throw existingError
        existing.push(...(data || []))
      }
      const duplicates = new Set(existing.map(r => r.employee_number))
      if (duplicates.size) throw new Error('Import stopped; employee numbers already exist: ' + [...duplicates].slice(0, 10).join(', '))
      const payload = csvRows.map(({ line, ...r }) => ({ organisation_id: organisationId, ...r }))
      const { error: importError } = await supabase.from('employees').insert(payload)
      if (importError) throw importError
      setNotice(`Imported ${payload.length} employee records. No roles or assignments were inferred.`)
      setCsvRows([]); setCsvErrors([]); setCsvFileName('')
      await load()
    } catch (e) { setError(e.message || 'Employee import failed; no partial retry should be attempted until the error is reviewed.') }
    finally { setBusy(false) }
  }

  async function createRole(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    let createdRole = null
    try {
      const payload = { organisation_id: organisationId, role_key: role.role_key.trim().toLowerCase().replace(/[^a-z0-9_]+/g, '_'), name: role.name.trim(), description: role.description.trim() || null, is_system_role: false }
      const { data, error: roleError } = await supabase.from('roles').insert(payload).select('id').single()
      if (roleError) throw roleError
      createdRole = data
      if (role.permissions.length) {
        const rows = role.permissions.map(permission_key => ({ role_id: data.id, permission_key }))
        const { error: permissionError } = await supabase.from('role_permissions').insert(rows)
        if (permissionError) throw new Error('Role created, but permission grants failed: ' + permissionError.message)
      }
      setRole({ role_key: '', name: '', description: '', permissions: [] })
      setNotice('Role and selected permission grants saved.')
      await load()
    } catch (e) {
      setError(e.message || (createdRole ? 'Role created, but permission grants failed.' : 'Could not create role.'))
      if (createdRole) await load()
    } finally { setBusy(false) }
  }

  async function endAssignment(id, startsAt) {
    if (!window.confirm('End this assignment now? Its history will be retained.')) return
    setBusy(true); setError(''); setNotice('')
    try {
      const endAt = new Date(Math.max(Date.now(), new Date(startsAt).getTime() + 1000)).toISOString()
      const { error: updateError } = await supabase.from('employee_assignments').update({ status: 'revoked', ends_at: endAt }).eq('id', id)
      if (updateError) throw updateError
      setNotice('Assignment ended; its history is retained.')
      await load()
    } catch (e) { setError(e.message || 'Could not end assignment.') }
    finally { setBusy(false) }
  }

  const selectOptions = (items, key, label) => items.map(item => <option key={item.id} value={item.id}>{item[key] || item[label]}</option>)
  const employeeOptions = employees.filter(e => e.status === 'active')

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark small">M</div><div><strong>MobiWave CNC</strong><span>Organisation settings</span></div></div>
      <div className="side-label">WORKSPACE</div>
      <button className="nav" onClick={onBack}><span className="nav-icon">▦</span>Finance overview</button>
      <button className="nav active"><span className="nav-icon">⌘</span>Organisation configuration</button>
      <div className="sidebar-bottom"><div className="user-chip"><div className="avatar">{(session?.user?.email || 'U').slice(0,1).toUpperCase()}</div><div className="user-meta"><strong>{session?.user?.email}</strong><span>Organisation workspace</span></div></div><button className="signout" onClick={onSignOut}>Sign out <span>↗</span></button></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><div><p className="eyebrow">WORKSPACE / CONFIGURATION</p><h1>Organisation structure</h1><p className="muted">Configure units, departments, roles, assignments and reporting relationships without hard-coded company hierarchies.</p></div><button className="secondary" onClick={load} disabled={busy}>{busy ? 'Refreshing…' : '↻ Refresh'}</button></header>
      {error && <div className="notice" role="alert"><strong>Could not complete action</strong><span>{error}</span><button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}
      {notice && <div className="success-notice" role="status"><strong>Saved</strong><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      <section className="stat-grid">
        <article className="stat"><span className="stat-label">Units / departments</span><strong>{units.length}</strong><span className="stat-note">Hierarchical, parent-linked</span></article>
        <article className="stat"><span className="stat-label">Roles</span><strong>{roles.length}</strong><span className="stat-note">Permissions remain policy-controlled</span></article>
        <article className="stat"><span className="stat-label">Employees</span><strong>{employees.length}</strong><span className="stat-note">Staff records in this workspace</span></article>
        <article className="stat"><span className="stat-label">Active assignments</span><strong>{assignments.filter(a => a.status === 'active' && (!a.ends_at || new Date(a.ends_at) > new Date())).length}</strong><span className="stat-note">Multiple concurrent assignments supported</span></article>
      </section>
      <section className="table-panel">
        <div className="panel-head"><div><h2>Configuration areas</h2><p className="muted">Writes are enforced by the organisation’s Supabase row-level policies.</p></div><span className="record-count">{organisationId ? 'Workspace linked' : 'Resolving workspace'}</span></div>
        <div className="table-tools">{TABS.map(t => <button key={t.id} className={tab === t.id ? 'primary' : 'secondary'} onClick={() => setTab(t.id)}>{t.label}</button>)}</div>
        {tab === 'units' && <>
          <form className="admin-create-form transition-form" onSubmit={e => save(e, 'org_units', { ...unit, parent_id: unit.parent_id || null, metadata: {} }, () => setUnit({ unit_type: 'shop', code: '', name: '', parent_id: '' }))}>
            <label>Unit type<input required maxLength={80} value={unit.unit_type} onChange={e => setUnit(v => ({ ...v, unit_type: e.target.value }))} placeholder="head_office, zone, region, shop, department" /></label>
            <label>Unique code<input required maxLength={80} value={unit.code} onChange={e => setUnit(v => ({ ...v, code: e.target.value.trim().toUpperCase() }))} placeholder="ZONE-01" /></label>
            <label>Unit name<input required maxLength={160} value={unit.name} onChange={e => setUnit(v => ({ ...v, name: e.target.value }))} placeholder="Coast Zone" /></label>
            <label>Parent unit<select value={unit.parent_id} onChange={e => setUnit(v => ({ ...v, parent_id: e.target.value }))}><option value="">No parent / top level</option>{units.map(x => <option key={x.id} value={x.id}>{x.name} · {x.unit_type}</option>)}</select></label>
            <button className="primary" disabled={busy || !organisationId}>Add unit / department</button>
          </form>
          <div className="table-scroll"><table><thead><tr><th>Unit</th><th>Type</th><th>Parent</th><th>Code</th><th>Status</th></tr></thead><tbody>{units.map(x => <tr key={x.id}><td><strong>{x.name}</strong></td><td>{x.unit_type}</td><td>{unitNames[x.parent_id] || '—'}</td><td>{x.code}</td><td>{x.status}</td></tr>)}</tbody></table>{!units.length && <div className="empty"><strong>No organisation units</strong><p>Add a head office, zone, region, shop or department to begin.</p></div>}</div>
        </>}
        {tab === 'employees' && <>
          <div className="import-intro"><h3>Employee register import</h3><p>Upload a verified staff CSV. This imports employee identity records only; role assignments and reporting lines must be configured separately.</p><p>Required columns: <code>employee_number,full_name</code>. Optional columns: <code>email,phone_e164,status</code>.</p><button className="secondary" type="button" onClick={() => { const csv = 'employee_number,full_name,email,phone_e164,status\nEMP-001,Example Employee,employee@example.com,+254700000000,active\n'; const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' })); const a = document.createElement('a'); a.href = url; a.download = 'cnc-employees-template.csv'; a.click(); URL.revokeObjectURL(url) }}>Download CSV template</button></div>
          <div className="import-upload"><label>Choose employee CSV<input type="file" accept=".csv,text/csv" onChange={readEmployeeCsv} /></label>{csvFileName && <span>{csvFileName} · {csvRows.length} rows parsed</span>}</div>
          {!!csvErrors.length && <div className="notice" role="alert"><strong>Fix the CSV before importing</strong><ul>{csvErrors.slice(0,12).map((e,i) => <li key={i}>{e}</li>)}</ul></div>}
          {!!csvRows.length && !csvErrors.length && <><div className="panel-head"><div><h3>Import preview</h3><p className="muted">Review the first 10 of {csvRows.length} parsed rows. Existing employee numbers will be checked before insert.</p></div><button className="primary" disabled={busy || !organisationId} onClick={importEmployees}>Import {csvRows.length} employees</button></div><div className="table-scroll"><table><thead><tr><th>Employee number</th><th>Full name</th><th>Email</th><th>Phone</th><th>Status</th></tr></thead><tbody>{csvRows.slice(0,10).map(r => <tr key={r.line}><td>{r.employee_number}</td><td>{r.full_name}</td><td>{r.email || '—'}</td><td>{r.phone_e164 || '—'}</td><td>{r.status}</td></tr>)}</tbody></table></div></>}
          <div className="table-scroll"><table><thead><tr><th>Employee number</th><th>Full name</th><th>Email</th><th>Status</th></tr></thead><tbody>{employees.map(e => <tr key={e.id}><td>{e.employee_number}</td><td><strong>{e.full_name}</strong></td><td>{e.email || '—'}</td><td>{e.status}</td></tr>)}</tbody></table></div>
        </>}
        {tab === 'roles' && <>
          <form className="admin-create-form transition-form" onSubmit={createRole}>
            <label>Role key<input required maxLength={80} value={role.role_key} onChange={e => setRole(v => ({ ...v, role_key: e.target.value }))} placeholder="shop_manager" /></label>
            <label>Role name<input required maxLength={120} value={role.name} onChange={e => setRole(v => ({ ...v, name: e.target.value }))} placeholder="Shop Manager" /></label>
            <label>Description<textarea maxLength={2000} value={role.description} onChange={e => setRole(v => ({ ...v, description: e.target.value }))} placeholder="Responsibilities and scope" /></label>
            <fieldset className="permission-picker"><legend>Permission grants</legend>{PERMISSIONS.map(([key, description]) => <label key={key} className="permission-option"><input type="checkbox" checked={role.permissions.includes(key)} onChange={e => setRole(v => ({ ...v, permissions: e.target.checked ? [...v.permissions, key] : v.permissions.filter(p => p !== key) }))} /><span><strong>{description}</strong><small>{key}</small></span></label>)}</fieldset>
            <button className="primary" disabled={busy || !organisationId}>Create role and permissions</button>
          </form>
          <div className="table-scroll"><table><thead><tr><th>Role</th><th>Key</th><th>Description</th><th>System role</th></tr></thead><tbody>{roles.map(x => <tr key={x.id}><td><strong>{x.name}</strong></td><td>{x.role_key}</td><td>{x.description || '—'}</td><td>{x.is_system_role ? 'Yes' : 'No'}</td></tr>)}</tbody></table></div>
        </>}
        {tab === 'assignments' && <>
          {employees.length && roles.length && units.length ? <form className="admin-create-form transition-form" onSubmit={e => save(e, 'employee_assignments', { ...assignment, starts_at: new Date(assignment.starts_at).toISOString(), ends_at: assignment.ends_at ? new Date(assignment.ends_at).toISOString() : null, is_acting: Boolean(assignment.is_acting), created_by: session.user.id }, () => setAssignment(v => ({ ...v, ends_at: '', is_acting: false })))}>
            <label>Employee<select required value={assignment.employee_id} onChange={e => setAssignment(v => ({ ...v, employee_id: e.target.value }))}>{selectOptions(employeeOptions, 'full_name', 'employee_number')}</select></label>
            <label>Role<select required value={assignment.role_id} onChange={e => setAssignment(v => ({ ...v, role_id: e.target.value }))}>{selectOptions(roles, 'name', 'role_key')}</select></label>
            <label>Unit / department<select required value={assignment.org_unit_id} onChange={e => setAssignment(v => ({ ...v, org_unit_id: e.target.value }))}>{selectOptions(units, 'name', 'code')}</select></label>
            <label>Effective from<input required type="date" value={assignment.starts_at} onChange={e => setAssignment(v => ({ ...v, starts_at: e.target.value }))} /></label>
            <label>Effective until (optional)<input type="date" value={assignment.ends_at} onChange={e => setAssignment(v => ({ ...v, ends_at: e.target.value }))} /></label>
            <label className="check-label"><input type="checkbox" checked={assignment.is_acting} onChange={e => setAssignment(v => ({ ...v, is_acting: e.target.checked }))} />Acting assignment</label>
            <button className="primary" disabled={busy || !organisationId}>Add assignment</button>
          </form> : <p className="empty">Create at least one employee, role and unit before assigning staff. Employee onboarding/import is managed separately.</p>}
          <div className="table-scroll"><table><thead><tr><th>Employee</th><th>Role</th><th>Unit</th><th>Effective period</th><th>Type / status</th><th>Action</th></tr></thead><tbody>{assignments.map(a => <tr key={a.id}><td>{employeeNames[a.employee_id] || a.employee_id}</td><td>{roleNames[a.role_id] || '—'}</td><td>{unitNames[a.org_unit_id] || '—'}</td><td>{fmtDate(a.starts_at)} – {fmtDate(a.ends_at)}</td><td>{a.is_acting ? 'Acting' : 'Substantive'} · {a.status}</td><td>{a.status === 'active' && <button className="table-action danger-action" onClick={() => endAssignment(a.id, a.starts_at)} disabled={busy}>End</button>}</td></tr>)}</tbody></table></div>
        </>}
        {tab === 'reporting' && <>
          {employeeOptions.length > 1 && units.length ? <form className="admin-create-form transition-form" onSubmit={e => save(e, 'reporting_relationships', { ...line, starts_at: new Date(line.starts_at).toISOString(), ends_at: line.ends_at ? new Date(line.ends_at).toISOString() : null, org_unit_id: line.org_unit_id || null }, () => setLine(v => ({ ...v, ends_at: '' })))}>
            <label>Employee<select required value={line.employee_id} onChange={e => setLine(v => ({ ...v, employee_id: e.target.value }))}>{selectOptions(employeeOptions, 'full_name', 'employee_number')}</select></label>
            <label>Reports to<select required value={line.manager_employee_id} onChange={e => setLine(v => ({ ...v, manager_employee_id: e.target.value }))}>{selectOptions(employeeOptions.filter(x => x.id !== line.employee_id), 'full_name', 'employee_number')}</select></label>
            <label>Relevant unit (optional)<select value={line.org_unit_id} onChange={e => setLine(v => ({ ...v, org_unit_id: e.target.value }))}><option value="">Not scoped to a unit</option>{selectOptions(units, 'name', 'code')}</select></label>
            <label>Effective from<input required type="date" value={line.starts_at} onChange={e => setLine(v => ({ ...v, starts_at: e.target.value }))} /></label>
            <label>Effective until (optional)<input type="date" value={line.ends_at} onChange={e => setLine(v => ({ ...v, ends_at: e.target.value }))} /></label>
            <button className="primary" disabled={busy || !organisationId || line.employee_id === line.manager_employee_id}>Add reporting line</button>
          </form> : <p className="empty">Create at least two active employees and a unit before defining reporting lines.</p>}
          <div className="table-scroll"><table><thead><tr><th>Employee</th><th>Manager</th><th>Unit</th><th>Effective period</th></tr></thead><tbody>{reporting.map(x => <tr key={x.id}><td>{employeeNames[x.employee_id] || x.employee_id}</td><td>{employeeNames[x.manager_employee_id] || x.manager_employee_id}</td><td>{unitNames[x.org_unit_id] || '—'}</td><td>{fmtDate(x.starts_at)} – {fmtDate(x.ends_at)}</td></tr>)}</tbody></table></div>
        </>}
      </section>
      <p className="fine-print">Organisation UUID: {organisationId || 'not resolved'}. Configuration changes are subject to tenant RLS and permission checks. Employee records and permission grants are intentionally not auto-created by this screen.</p>
      <footer className="app-footer">MobiWave Innovations Ltd <span>·</span> Configurable organisation model <span>·</span> Effective-dated assignments</footer>
    </main>
  </div>
}
