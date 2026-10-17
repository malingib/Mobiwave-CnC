import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { createClient } from '@supabase/supabase-js'
import './style.css'
import Landing from './Landing.jsx'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseKey = import.meta.env.VITE_SUPABASE_ANON_KEY
const supabase = supabaseUrl && supabaseKey ? createClient(supabaseUrl, supabaseKey) : null
const money = (value) => new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES', maximumFractionDigits: 2 }).format(Number(value || 0))
const date = (value) => value ? new Date(value).toLocaleDateString('en-KE', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'


function AdminDashboard({ supabase, session, onSignOut }) {
  const [clients, setClients] = useState([])
  const [selectedClient, setSelectedClient] = useState(null)
  const [clientExpenses, setClientExpenses] = useState([])
  const [companyName, setCompanyName] = useState('MobiWave CNC Demo Company')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [admins, setAdmins] = useState([])
  const [adminEmail, setAdminEmail] = useState('')

  const callAdmin = useCallback(async (body) => {
    const { data, error: invokeError } = await supabase.functions.invoke('cnc-admin-console', { body })
    if (invokeError) throw new Error(invokeError.message || 'Admin request failed.')
    if (data?.error) throw new Error(data.error)
    return data
  }, [supabase])

  const loadAdmins = useCallback(async () => {
    try { const data = await callAdmin({ action: 'list_admins' }); setAdmins(data.admins || []) }
    catch (e) { setError(e.message || 'Could not load administrators.') }
  }, [callAdmin])

  async function inviteAdmin(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try { const data = await callAdmin({ action: 'invite_admin', email: adminEmail.trim() }); setNotice(data.message || 'Administrator access updated.'); setAdminEmail(''); await loadAdmins() }
    catch (e) { setError(e.message || 'Could not add administrator.') }
    finally { setBusy(false) }
  }

  const loadClients = useCallback(async () => {
    setBusy(true); setError('')
    try {
      const data = await callAdmin({ action: 'list_clients' })
      setClients(data.clients || [])
    } catch (e) { setError(e.message || 'Could not load client organisations.') }
    finally { setBusy(false) }
  }, [callAdmin])

  useEffect(() => { loadClients(); loadAdmins() }, [loadClients, loadAdmins])

  async function changeClientStatus(client) {
    const nextStatus = client.status === 'suspended' ? 'active' : 'suspended'
    if (!window.confirm((nextStatus === 'suspended' ? 'Suspend ' : 'Reactivate ') + client.name + '?')) return
    setBusy(true); setError(''); setNotice('')
    try { const result = await callAdmin({ action: 'set_client_status', organisationId: client.id, status: nextStatus }); setNotice(result.message); await loadClients() }
    catch (e) { setError(e.message || 'Could not change client status.') }
    finally { setBusy(false) }
  }

  async function createDemo(event) {
    event.preventDefault()
    setBusy(true); setError(''); setNotice('')
    try {
      const data = await callAdmin({ action: 'create_demo_client', name: companyName })
      setNotice(data.message || 'Demo client created.')
      setCompanyName('MobiWave CNC Demo Company')
      await loadClients()
    } catch (e) { setError(e.message || 'Could not create demo client.') }
    finally { setBusy(false) }
  }

  async function openClient(client) {
    setBusy(true); setError(''); setNotice('')
    try {
      const data = await callAdmin({ action: 'view_client', organisationId: client.id })
      setSelectedClient(data.client)
      setClientExpenses(data.expenses || [])
    } catch (e) { setError(e.message || 'Could not open client workspace.') }
    finally { setBusy(false) }
  }

  const totalValue = clientExpenses.reduce((sum, item) => sum + Number(item.gross_amount || 0), 0)
  const money = (value) => new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES', maximumFractionDigits: 2 }).format(Number(value || 0))
  const date = (value) => value ? new Date(value).toLocaleDateString('en-KE', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'

  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><div className="brand-mark small">M</div><div><strong>MobiWave CNC</strong><span>Platform administration</span></div></div><div className="side-label">ADMINISTRATION</div><button className={'nav ' + (!selectedClient ? 'active' : '')} onClick={() => { setSelectedClient(null); window.location.hash = '' }}><span className="nav-icon">▦</span>Client organisations</button><button className="nav" onClick={() => window.location.hash = 'billing'}><span className="nav-icon">▤</span>Billing & subscriptions</button><button className="nav" onClick={() => window.location.hash = 'plans'}><span className="nav-icon">◇</span>Plans & limits</button><button className="nav" onClick={() => window.location.hash = 'payments'}><span className="nav-icon">↔</span>Payments</button><button className="nav" onClick={() => window.location.hash = 'audit'}><span className="nav-icon">≡</span>Audit logs</button><button className="nav" onClick={() => window.location.hash = 'profile'}><span className="nav-icon">◉</span>My profile</button>{selectedClient && <button className="nav active"><span className="nav-icon">◫</span>Client support view</button>}<div className="sidebar-bottom"><div className="user-chip"><div className="avatar">S</div><div className="user-meta"><strong>{session.user.email}</strong><span>Super administrator</span></div></div><button className="signout" onClick={onSignOut}>Sign out <span>↗</span></button></div></aside>
    <main className="main-area">
      <header className="topbar"><div><p className="eyebrow">MOBIWAVE CNC / ADMIN</p><h1>{selectedClient ? selectedClient.name : 'Super admin dashboard'}</h1><p className="muted">{selectedClient ? 'Read-only support view of this client’s workspace.' : 'Manage admitted client organisations and prepare demonstrations.'}</p></div><button className="secondary" onClick={selectedClient ? () => setSelectedClient(null) : loadClients} disabled={busy}>{selectedClient ? '← All clients' : busy ? 'Refreshing…' : '↻ Refresh clients'}</button></header>
      {error && <div className="notice"><strong>Action could not be completed</strong><span>{error}</span><button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}
      {notice && <div className="success-notice"><strong>Done</strong><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {!selectedClient ? <>
        <section className="stat-grid"><article className="stat"><span className="stat-label">Client organisations</span><strong>{clients.length}</strong><span className="stat-note">Across this single CNC instance</span></article><article className="stat"><span className="stat-label">Active clients</span><strong>{clients.filter(x => x.status === 'active').length}</strong><span className="stat-note">Access managed by MobiWave</span></article><article className="stat"><span className="stat-label">Demo clients</span><strong>{clients.filter(x => x.is_demo).length}</strong><span className="stat-note">Illustrative data only</span></article><article className="stat"><span className="stat-label">Admission model</span><strong className="amount">By request</strong><span className="stat-note">No public self-registration</span></article></section>
        <section className="table-panel admin-create-panel"><div className="panel-head"><div><h2>Super administrators</h2><p className="muted">Invite trusted team members to manage CNC. They receive full platform administration privileges.</p></div><span className="record-count">{admins.length} administrators</span></div><form className="admin-create-form" onSubmit={inviteAdmin}><label>Administrator email<input type="email" value={adminEmail} onChange={e => setAdminEmail(e.target.value)} placeholder="admin@company.com" maxLength={254} required /></label><button className="primary" disabled={busy}>{busy ? 'Working…' : '＋ Add administrator'}</button></form><div className="admin-list">{admins.map(admin => <div className="admin-list-item" key={admin.id}><div><strong>{admin.email || 'Pending email'}</strong><small>{admin.confirmed ? 'Account confirmed' : 'Invitation / confirmation pending'}</small></div><span className="pill good">Super admin</span></div>)}{!admins.length && <p className="muted">No super administrators were returned. Refresh after the initial administrator is configured.</p>}</div></section>
        <section className="table-panel admin-create-panel"><div className="panel-head"><div><h2>Create a demo client</h2><p className="muted">Creates a separate demo organisation with clearly illustrative sample expenses. It does not provision a real customer account.</p></div></div><form className="admin-create-form" onSubmit={createDemo}><label>Demo company name<input value={companyName} onChange={e => setCompanyName(e.target.value)} maxLength={120} required /></label><button className="primary" disabled={busy}>{busy ? 'Working…' : '＋ Create demo client'}</button></form></section>
        <section className="table-panel"><div className="panel-head"><div><h2>Client organisations</h2><p className="muted">New companies must contact MobiWave and be admitted by an administrator.</p></div><span className="record-count">{clients.length} organisations</span></div><div className="table-scroll"><table><thead><tr><th>Organisation</th><th>Type</th><th>Status</th><th>Expense records</th><th>Created</th><th>Actions</th></tr></thead><tbody>{clients.map(client => <tr key={client.id}><td><strong>{client.name}</strong><small>{client.slug}</small></td><td>{client.is_demo ? <span className="pill pending">Demo</span> : 'Client'}</td><td><span className={'pill ' + (client.status === 'active' ? 'good' : 'bad')}>{client.status}</span></td><td>{client.expense_count || 0}</td><td>{date(client.created_at)}</td><td><button className="table-action" onClick={() => openClient(client)} disabled={busy}>View ↗</button> <button className="table-action danger-action" onClick={() => changeClientStatus(client)} disabled={busy}>{client.status === 'suspended' ? 'Reactivate' : 'Suspend'}</button></td></tr>)}</tbody></table>{!clients.length && <div className="empty"><strong>{busy ? 'Loading client organisations…' : 'No client organisations yet'}</strong><p>Create a demo client to preview the workflow, or admit a company after its access request has been reviewed.</p></div>}</div></section>
      </> : <>
        <div className="support-banner"><strong>Support view · Read only</strong><span>You are viewing this client’s data as a super administrator. No client credentials are being used, and changes are disabled in this view.</span><button className="secondary" onClick={() => setSelectedClient(null)}>Exit client view</button></div>
        <section className="stat-grid"><article className="stat"><span className="stat-label">Expense records</span><strong>{clientExpenses.length}</strong><span className="stat-note">Latest 250 records</span></article><article className="stat"><span className="stat-label">Needs attention</span><strong>{clientExpenses.filter(x => ['submitted','needs_review'].includes(x.status) || ['needs_review','verification_failed','awaiting_verification'].includes(x.verification_status)).length}</strong><span className="stat-note warning-text">Review or verification pending</span></article><article className="stat"><span className="stat-label">Accepted / reconciled</span><strong>{clientExpenses.filter(x => ['accepted','reconciled'].includes(x.status)).length}</strong><span className="stat-note">Completed review status</span></article><article className="stat"><span className="stat-label">Total listed value</span><strong className="amount">{money(totalValue)}</strong><span className="stat-note">Across loaded records</span></article></section>
        <section className="table-panel"><div className="panel-head"><div><h2>Client expense register</h2><p className="muted">Read-only support access.</p></div><span className="record-count">{clientExpenses.length} records</span></div><div className="table-scroll"><table><thead><tr><th>Merchant</th><th>Category</th><th>Expense date</th><th>Amount</th><th>Status</th><th>Invoice check</th></tr></thead><tbody>{clientExpenses.map(item => <tr key={item.id}><td><strong>{item.merchant_name || 'Unnamed merchant'}</strong><small>{String(item.id).slice(0,8).toUpperCase()}</small></td><td className="capitalize">{item.category || '—'}</td><td>{date(item.expense_date)}</td><td className="amount">{money(item.gross_amount)}</td><td><span className={'pill ' + (['accepted','reconciled'].includes(item.status) ? 'good' : item.status === 'rejected' ? 'bad' : 'pending')}>{(item.status || 'unknown').replaceAll('_',' ')}</span></td><td><span className={'pill ' + (item.verification_status === 'externally_verified' ? 'good' : 'pending')}>{(item.verification_status || 'unknown').replaceAll('_',' ')}</span></td></tr>)}</tbody></table>{!clientExpenses.length && <div className="empty"><strong>No expense records</strong><p>This organisation has no expenses to display yet.</p></div>}</div></section>
      </>}
      <footer className="app-footer">MobiWave Innovations Ltd <span>·</span> Super admin <span>·</span> Client admission by request</footer>
    </main>
  </div>
}



function PlatformAdminPage({ page, supabase, session, onSignOut, onNavigate }) {
  const [data, setData] = useState({ plans: [], clients: [], subscriptions: [], invoices: [], payments: [], totals: {} })
  const [logs, setLogs] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [plan, setPlan] = useState({ planKey: '', name: '', description: '', monthlyPrice: '0', annualPrice: '0', maxUsers: '', maxOrgUnits: '', maxMonthlyExpenses: '', isActive: false })
  const [editingPlan, setEditingPlan] = useState(null)
  const [invoice, setInvoice] = useState({ organisationId: '', description: 'MobiWave CNC subscription', amount: '', taxAmount: '0', dueAt: '' })
  const [payment, setPayment] = useState({ organisationId: '', invoiceId: '', amount: '', method: 'mpesa', providerReference: '', payerReference: '', notes: '' })
  const [subscriptionDrafts, setSubscriptionDrafts] = useState({})
  const money = (v) => new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES', maximumFractionDigits: 2 }).format(Number(v || 0))
  const fmtDate = (v) => v ? new Date(v).toLocaleDateString('en-KE', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
  const callAdmin = useCallback(async (body) => {
    const { data: result, error: invokeError } = await supabase.functions.invoke('cnc-admin-console', { body })
    if (invokeError) throw new Error(invokeError.message || 'Admin request failed.')
    if (result?.error) throw new Error(result.error)
    return result
  }, [supabase])
  const load = useCallback(async () => {
    setBusy(true); setError('')
    try {
      const result = await callAdmin({ action: 'list_billing' })
      setData(result)
      setSubscriptionDrafts(Object.fromEntries((result.clients || []).map(c => [c.id, {
        planId: c.subscription?.plan_id || '', status: c.subscription?.status || 'trial',
        billingCycle: c.subscription?.billing_cycle || 'monthly', customPrice: c.subscription?.custom_price ?? '',
        currentPeriodEnd: c.subscription?.current_period_end ? c.subscription.current_period_end.slice(0,10) : '',
        trialEndsAt: c.subscription?.trial_ends_at ? c.subscription.trial_ends_at.slice(0,10) : '',
        notes: c.subscription?.notes || ''
      }])))
      if (!invoice.organisationId && result.clients?.length) setInvoice(v => ({ ...v, organisationId: result.clients[0].id }))
      if (!payment.organisationId && result.clients?.length) setPayment(v => ({ ...v, organisationId: result.clients[0].id }))
    } catch (e) { setError(e.message || 'Could not load platform billing data.') }
    finally { setBusy(false) }
  }, [callAdmin])
  const loadLogs = useCallback(async () => {
    setBusy(true); setError('')
    try { const result = await callAdmin({ action: 'list_audit_logs' }); setLogs(result.logs || []) }
    catch (e) { setError(e.message || 'Could not load audit logs.') }
    finally { setBusy(false) }
  }, [callAdmin])
  useEffect(() => { if (page === 'audit') loadLogs(); else load() }, [page, load, loadLogs])
  async function act(action, payload, successMessage) {
    setBusy(true); setError(''); setNotice('')
    try { const result = await callAdmin({ action, ...payload }); setNotice(result.message || successMessage || 'Changes saved.'); await (page === 'audit' ? loadLogs() : load()) }
    catch (e) { setError(e.message || 'The action failed.') }
    finally { setBusy(false) }
  }
  async function savePlan(e) {
    e.preventDefault()
    const payload = { ...plan, monthlyPrice: Number(plan.monthlyPrice), annualPrice: Number(plan.annualPrice),
      maxUsers: plan.maxUsers === '' ? null : Number(plan.maxUsers), maxOrgUnits: plan.maxOrgUnits === '' ? null : Number(plan.maxOrgUnits),
      maxMonthlyExpenses: plan.maxMonthlyExpenses === '' ? null : Number(plan.maxMonthlyExpenses), planId: editingPlan || undefined }
    await act('upsert_plan', payload, 'Subscription plan saved.')
    setPlan({ planKey: '', name: '', description: '', monthlyPrice: '0', annualPrice: '0', maxUsers: '', maxOrgUnits: '', maxMonthlyExpenses: '', isActive: false }); setEditingPlan(null)
  }
  function editPlan(p) {
    setEditingPlan(p.id); setPlan({ planKey: p.plan_key, name: p.name, description: p.description || '', monthlyPrice: String(p.monthly_price), annualPrice: String(p.annual_price),
      maxUsers: p.max_users ?? '', maxOrgUnits: p.max_org_units ?? '', maxMonthlyExpenses: p.max_monthly_expenses ?? '', isActive: p.is_active })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  async function saveSubscription(orgId) {
    const d = subscriptionDrafts[orgId] || {}
    await act('upsert_subscription', { organisationId: orgId, planId: d.planId || null, status: d.status || 'trial', billingCycle: d.billingCycle || 'monthly',
      customPrice: d.customPrice === '' || d.customPrice == null ? null : Number(d.customPrice),
      currentPeriodEnd: d.currentPeriodEnd || null, trialEndsAt: d.trialEndsAt || null, notes: d.notes || '' }, 'Client subscription updated.')
  }
  async function createInvoice(e) {
    e.preventDefault()
    await act('create_invoice', { ...invoice, amount: Number(invoice.amount), taxAmount: Number(invoice.taxAmount || 0), dueAt: invoice.dueAt || null }, 'Invoice created.')
    setInvoice(v => ({ ...v, description: 'MobiWave CNC subscription', amount: '', taxAmount: '0', dueAt: '' }))
  }
  async function recordPayment(e) {
    e.preventDefault()
    await act('record_payment', { ...payment, amount: Number(payment.amount), invoiceId: payment.invoiceId || null }, 'Payment recorded as pending verification.')
    setPayment(v => ({ ...v, invoiceId: '', amount: '', providerReference: '', payerReference: '', notes: '' }))
  }
  const title = ({ billing: 'Billing & subscriptions', plans: 'Subscription plans', payments: 'Payments & reconciliation', audit: 'Audit logs' })[page] || 'Platform administration'
  const description = ({ billing: 'Assign client plans, manage trial periods and subscription status.', plans: 'Configure prices and usage limits before making a plan available.', payments: 'Record payment references and confirm receipts only after independent verification.', audit: 'Review administrative actions and billing events.' })[page] || ''
  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><div className="brand-mark small">M</div><div><strong>MobiWave CNC</strong><span>Platform administration</span></div></div><div className="side-label">PLATFORM</div>
      <button className="nav" onClick={() => onNavigate('overview')}><span className="nav-icon">▦</span>Overview & clients</button>
      <button className={'nav ' + (page === 'billing' ? 'active' : '')} onClick={() => onNavigate('billing')}><span className="nav-icon">▤</span>Billing & subscriptions</button>
      <button className={'nav ' + (page === 'plans' ? 'active' : '')} onClick={() => onNavigate('plans')}><span className="nav-icon">◇</span>Plans & limits</button>
      <button className={'nav ' + (page === 'payments' ? 'active' : '')} onClick={() => onNavigate('payments')}><span className="nav-icon">↔</span>Payments</button>
      <button className={'nav ' + (page === 'audit' ? 'active' : '')} onClick={() => onNavigate('audit')}><span className="nav-icon">≡</span>Audit logs</button>
      <button className="nav" onClick={() => onNavigate('profile')}><span className="nav-icon">◉</span>My profile</button>
      <div className="sidebar-bottom"><div className="user-chip"><div className="avatar">S</div><div className="user-meta"><strong>{session.user.email}</strong><span>Super administrator</span></div></div><button className="signout" onClick={onSignOut}>Sign out <span>↗</span></button></div>
    </aside>
    <main className="main-area"><header className="topbar"><div><p className="eyebrow">MOBIWAVE CNC / PLATFORM</p><h1>{title}</h1><p className="muted">{description}</p></div><button className="secondary" onClick={page === 'audit' ? loadLogs : load} disabled={busy}>{busy ? 'Refreshing…' : '↻ Refresh'}</button></header>
      {error && <div className="notice"><strong>Action could not be completed</strong><span>{error}</span><button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}
      {notice && <div className="success-notice"><strong>Updated</strong><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {page === 'billing' && <>
        <section className="stat-grid"><article className="stat"><span className="stat-label">Client organisations</span><strong>{data.totals?.clients || 0}</strong><span className="stat-note">Across CNC</span></article><article className="stat"><span className="stat-label">Active clients</span><strong>{data.totals?.activeClients || 0}</strong><span className="stat-note">Organisation status</span></article><article className="stat"><span className="stat-label">Invoices issued</span><strong>{money(data.totals?.billed)}</strong><span className="stat-note">Includes unpaid invoices</span></article><article className="stat"><span className="stat-label">Collected</span><strong className="amount">{money(data.totals?.collected)}</strong><span className="stat-note">Confirmed payment records only</span></article></section>
        <section className="table-panel"><div className="panel-head"><div><h2>Client subscriptions</h2><p className="muted">Assign plan and access status per organisation. Saving a subscription does not create an invoice or confirm payment.</p></div><span className="record-count">{data.clients.length} clients</span></div><div className="table-scroll"><table><thead><tr><th>Client</th><th>Plan</th><th>Status</th><th>Cycle</th><th>Custom KES price</th><th>Period end</th><th>Action</th></tr></thead><tbody>{data.clients.map(c => { const d = subscriptionDrafts[c.id] || {}; return <tr key={c.id}><td><strong>{c.name}</strong><small>{c.status} · {c.slug}</small></td><td><select value={d.planId || ''} onChange={e => setSubscriptionDrafts(p => ({ ...p, [c.id]: { ...d, planId: e.target.value } }))}><option value="">No plan</option>{data.plans.map(p => <option key={p.id} value={p.id}>{p.name}{p.is_active ? '' : ' (inactive)'}</option>)}</select></td><td><select value={d.status || 'trial'} onChange={e => setSubscriptionDrafts(p => ({ ...p, [c.id]: { ...d, status: e.target.value } }))}>{['trial','active','past_due','suspended','cancelled','expired'].map(v => <option key={v}>{v}</option>)}</select></td><td><select value={d.billingCycle || 'monthly'} onChange={e => setSubscriptionDrafts(p => ({ ...p, [c.id]: { ...d, billingCycle: e.target.value } }))}>{['monthly','annual','custom'].map(v => <option key={v}>{v}</option>)}</select></td><td><input className="compact-input" type="number" min="0" step="0.01" value={d.customPrice ?? ''} onChange={e => setSubscriptionDrafts(p => ({ ...p, [c.id]: { ...d, customPrice: e.target.value } }))} placeholder="Plan price" /></td><td><input className="compact-input" type="date" value={d.currentPeriodEnd || ''} onChange={e => setSubscriptionDrafts(p => ({ ...p, [c.id]: { ...d, currentPeriodEnd: e.target.value } }))} /></td><td><button className="table-action" disabled={busy} onClick={() => saveSubscription(c.id)}>Save subscription</button></td></tr> })}</tbody></table>{!data.clients.length && <div className="empty"><strong>No clients found</strong><p>Admit a client organisation first.</p></div>}</div></section>
        <section className="table-panel"><div className="panel-head"><div><h2>Invoices</h2><p className="muted">Create invoices for subscriptions, onboarding or agreed service charges.</p></div><button className="secondary" onClick={() => onNavigate('payments')}>View payments →</button></div><form className="admin-create-form invoice-form" onSubmit={createInvoice}><label>Client<select required value={invoice.organisationId} onChange={e => setInvoice(v => ({ ...v, organisationId: e.target.value }))}><option value="">Choose client</option>{data.clients.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label><label>Description<input required maxLength={500} value={invoice.description} onChange={e => setInvoice(v => ({ ...v, description: e.target.value }))} /></label><label>Amount before tax (KES)<input type="number" min="0.01" step="0.01" required value={invoice.amount} onChange={e => setInvoice(v => ({ ...v, amount: e.target.value }))} /></label><label>Tax (KES)<input type="number" min="0" step="0.01" value={invoice.taxAmount} onChange={e => setInvoice(v => ({ ...v, taxAmount: e.target.value }))} /></label><label>Due date<input type="date" value={invoice.dueAt} onChange={e => setInvoice(v => ({ ...v, dueAt: e.target.value ? new Date(e.target.value + 'T12:00:00').toISOString() : '' }))} /></label><button className="primary" disabled={busy || !data.clients.length}>＋ Create invoice</button></form>
          <div className="table-scroll"><table><thead><tr><th>Invoice</th><th>Client</th><th>Issued</th><th>Due</th><th>Total</th><th>Paid</th><th>Status</th><th>Action</th></tr></thead><tbody>{data.invoices.map(i => <tr key={i.id}><td><strong>{i.invoice_number}</strong><small>{i.description}</small></td><td>{data.clients.find(c => c.id === i.organisation_id)?.name || 'Unknown client'}</td><td>{fmtDate(i.issued_at)}</td><td>{fmtDate(i.due_at)}</td><td className="amount">{money(i.total_amount)}</td><td>{money(i.amount_paid)}</td><td><span className={'pill ' + (i.status === 'paid' ? 'good' : i.status === 'void' ? 'bad' : 'pending')}>{i.status}</span></td><td>{!['paid','void'].includes(i.status) && Number(i.amount_paid) === 0 ? <button className="table-action danger-action" disabled={busy} onClick={() => act('void_invoice', { entityId: i.id }, 'Invoice voided.')}>Void</button> : '—'}</td></tr>)}</tbody></table></div>
        </section>
      </>}
      {page === 'plans' && <>
        <section className="table-panel"><div className="panel-head"><div><h2>{editingPlan ? 'Edit plan' : 'Create subscription plan'}</h2><p className="muted">Plan limits are stored for platform administration. Runtime enforcement in client workflows must be applied separately before selling a plan.</p></div>{editingPlan && <button className="secondary" onClick={() => { setEditingPlan(null); setPlan({ planKey:'',name:'',description:'',monthlyPrice:'0',annualPrice:'0',maxUsers:'',maxOrgUnits:'',maxMonthlyExpenses:'',isActive:false }) }}>Cancel edit</button>}</div>
          <form className="admin-create-form plan-form" onSubmit={savePlan}><label>Plan key<input required maxLength={60} pattern="[a-zA-Z0-9_-]+" value={plan.planKey} onChange={e => setPlan(v => ({ ...v, planKey: e.target.value }))} placeholder="starter" /></label><label>Plan name<input required maxLength={100} value={plan.name} onChange={e => setPlan(v => ({ ...v, name: e.target.value }))} placeholder="Starter" /></label><label>Monthly price (KES)<input required type="number" min="0" step="0.01" value={plan.monthlyPrice} onChange={e => setPlan(v => ({ ...v, monthlyPrice: e.target.value }))} /></label><label>Annual price (KES)<input required type="number" min="0" step="0.01" value={plan.annualPrice} onChange={e => setPlan(v => ({ ...v, annualPrice: e.target.value }))} /></label><label>Maximum users<input type="number" min="0" value={plan.maxUsers} onChange={e => setPlan(v => ({ ...v, maxUsers: e.target.value }))} placeholder="Unlimited" /></label><label>Maximum org units<input type="number" min="0" value={plan.maxOrgUnits} onChange={e => setPlan(v => ({ ...v, maxOrgUnits: e.target.value }))} placeholder="Unlimited" /></label><label>Monthly expense limit<input type="number" min="0" value={plan.maxMonthlyExpenses} onChange={e => setPlan(v => ({ ...v, maxMonthlyExpenses: e.target.value }))} placeholder="Unlimited" /></label><label className="check-label"><input type="checkbox" checked={plan.isActive} onChange={e => setPlan(v => ({ ...v, isActive: e.target.checked }))} /> Available for assignment</label><label className="wide-field">Description<input maxLength={500} value={plan.description} onChange={e => setPlan(v => ({ ...v, description: e.target.value }))} /></label><button className="primary" disabled={busy}>{editingPlan ? 'Save changes' : 'Create plan'}</button></form></section>
        <section className="table-panel"><div className="panel-head"><div><h2>Configured plans</h2><p className="muted">Inactive plans can still remain assigned to existing subscriptions.</p></div><span className="record-count">{data.plans.length} plans</span></div><div className="table-scroll"><table><thead><tr><th>Plan</th><th>Monthly</th><th>Annual</th><th>User limit</th><th>Org unit limit</th><th>Expense limit</th><th>Status</th><th></th></tr></thead><tbody>{data.plans.map(p => <tr key={p.id}><td><strong>{p.name}</strong><small>{p.plan_key} · {p.description || 'No description'}</small></td><td>{money(p.monthly_price)}</td><td>{money(p.annual_price)}</td><td>{p.max_users ?? 'Unlimited'}</td><td>{p.max_org_units ?? 'Unlimited'}</td><td>{p.max_monthly_expenses ?? 'Unlimited'}</td><td><span className={'pill ' + (p.is_active ? 'good' : 'pending')}>{p.is_active ? 'Active' : 'Inactive'}</span></td><td><button className="table-action" onClick={() => editPlan(p)}>Edit</button></td></tr>)}</tbody></table>{!data.plans.length && <div className="empty"><strong>No plans configured</strong><p>Create a plan and verify its pricing and limits before activating it.</p></div>}</div></section>
      </>}
      {page === 'payments' && <>
        <section className="stat-grid"><article className="stat"><span className="stat-label">Collected</span><strong>{money(data.totals?.collected)}</strong><span className="stat-note">Confirmed only</span></article><article className="stat"><span className="stat-label">Outstanding invoices</span><strong>{money(data.totals?.outstanding)}</strong><span className="stat-note">Invoice total less recorded payments</span></article><article className="stat"><span className="stat-label">Payment records</span><strong>{data.payments.length}</strong><span className="stat-note">Latest 250</span></article><article className="stat"><span className="stat-label">Pending verification</span><strong>{data.payments.filter(p => p.status === 'pending').length}</strong><span className="stat-note warning-text">Not counted as collected</span></article></section>
        <section className="table-panel"><div className="panel-head"><div><h2>Record payment</h2><p className="muted">This records a claimed receipt as pending. It does not initiate an M-PESA STK push or verify a provider transaction.</p></div></div><form className="admin-create-form payment-form" onSubmit={recordPayment}><label>Client<select required value={payment.organisationId} onChange={e => setPayment(v => ({ ...v, organisationId: e.target.value, invoiceId: '' }))}><option value="">Choose client</option>{data.clients.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label><label>Invoice (optional)<select value={payment.invoiceId} onChange={e => { const inv=data.invoices.find(i=>i.id===e.target.value); setPayment(v=>({...v,invoiceId:e.target.value,amount:inv ? String(Math.max(0,Number(inv.total_amount)-Number(inv.amount_paid))) : v.amount})) }}><option value="">Unallocated payment</option>{data.invoices.filter(i => i.organisation_id === payment.organisationId && !['paid','void'].includes(i.status)).map(i => <option key={i.id} value={i.id}>{i.invoice_number} · balance {money(Number(i.total_amount)-Number(i.amount_paid))}</option>)}</select></label><label>Amount (KES)<input type="number" min="0.01" step="0.01" required value={payment.amount} onChange={e => setPayment(v => ({ ...v, amount: e.target.value }))} /></label><label>Method<select value={payment.method} onChange={e => setPayment(v => ({ ...v, method: e.target.value }))}>{[['mpesa','M-PESA'],['bank_transfer','Bank transfer'],['card','Card'],['cash','Cash'],['adjustment','Adjustment'],['other','Other']].map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label><label>Provider / transaction reference<input maxLength={120} value={payment.providerReference} onChange={e => setPayment(v => ({ ...v, providerReference: e.target.value }))} placeholder="e.g. M-PESA receipt code" /></label><label>Payer reference<input maxLength={120} value={payment.payerReference} onChange={e => setPayment(v => ({ ...v, payerReference: e.target.value }))} /></label><label className="wide-field">Notes<input maxLength={500} value={payment.notes} onChange={e => setPayment(v => ({ ...v, notes: e.target.value }))} /></label><button className="primary" disabled={busy || !data.clients.length}>＋ Record pending payment</button></form></section>
        <section className="table-panel"><div className="panel-head"><div><h2>Payment register</h2><p className="muted">Confirm only after checking the official M-PESA/bank/card provider record.</p></div></div><div className="table-scroll"><table><thead><tr><th>Recorded</th><th>Client / invoice</th><th>Method</th><th>Reference</th><th>Amount</th><th>Status</th><th>Action</th></tr></thead><tbody>{data.payments.map(p => <tr key={p.id}><td>{fmtDate(p.created_at)}</td><td><strong>{data.clients.find(c => c.id === p.organisation_id)?.name || 'Unknown client'}</strong><small>{data.invoices.find(i => i.id === p.invoice_id)?.invoice_number || 'Unallocated'}</small></td><td>{p.method}</td><td>{p.provider_reference || p.payer_reference || '—'}</td><td>{money(p.amount)}</td><td><span className={'pill ' + (p.status === 'confirmed' ? 'good' : p.status === 'failed' || p.status === 'reversed' ? 'bad' : 'pending')}>{p.status}</span></td><td>{p.status === 'pending' ? <button className="table-action" disabled={busy} onClick={() => { if (window.confirm('Confirm this payment only if receipt has been independently verified?')) act('confirm_payment', { paymentId: p.id }, 'Payment confirmed.') }}>Confirm receipt</button> : '—'}</td></tr>)}</tbody></table>{!data.payments.length && <div className="empty"><strong>No payment records yet</strong><p>Pending and confirmed payments will appear here.</p></div>}</div></section>
      </>}
      {page === 'audit' && <section className="table-panel"><div className="panel-head"><div><h2>Administrative activity</h2><p className="muted">Latest 500 recorded billing and platform events.</p></div><span className="record-count">{logs.length} events</span></div><div className="table-scroll"><table><thead><tr><th>Time</th><th>Actor</th><th>Action</th><th>Entity</th><th>Details</th></tr></thead><tbody>{logs.map(log => <tr key={log.id}><td>{new Date(log.created_at).toLocaleString('en-KE')}</td><td>{log.actor_email || log.actor_user_id || 'System'}</td><td><strong>{log.action}</strong></td><td>{log.entity_type} <small>{log.entity_id}</small></td><td><code className="audit-details">{JSON.stringify(log.details || {})}</code></td></tr>)}</tbody></table>{!logs.length && <div className="empty"><strong>No audit events found</strong><p>Actions will be logged as platform activity is recorded.</p></div>}</div></section>}
      <footer className="app-footer">MobiWave Innovations Ltd <span>·</span> Platform administration <span>·</span> Sensitive actions are audited</footer>
    </main>
  </div>
}

function WorkspacePage({ page, session, supabase, onBack, onSignOut }) {
  const [fullName, setFullName] = useState(session.user.user_metadata?.full_name || '')
  const [phone, setPhone] = useState(session.user.user_metadata?.phone || '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const titles = {
    billing: ['Billing & plans', 'Manage your subscription, invoices and account billing details.'],
    payments: ['Payments', 'Review payment history and transaction references.'],
    profile: ['My profile', 'Update your personal details and account preferences.'],
    settings: ['Workspace settings', 'Review your workspace access and support options.'],
    support: ['Help & support', 'Contact MobiWave for onboarding, billing or technical assistance.']
  }
  async function saveProfile(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    const { error: updateError } = await supabase.auth.updateUser({ data: { ...session.user.user_metadata, full_name: fullName.trim(), phone: phone.trim() } })
    if (updateError) setError(updateError.message)
    else setNotice('Profile details saved.')
    setBusy(false)
  }
  async function updatePassword(event) {
    event.preventDefault()
    const password = new FormData(event.currentTarget).get('newPassword')
    setBusy(true); setError(''); setNotice('')
    const { error: updateError } = await supabase.auth.updateUser({ password: String(password || '') })
    if (updateError) setError(updateError.message)
    else { setNotice('Password updated successfully.'); event.currentTarget.reset() }
    setBusy(false)
  }
  const [heading, description] = titles[page] || titles.settings
  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><div className="brand-mark small">M</div><div><strong>MobiWave CNC</strong><span>Finance operations</span></div></div><div className="side-label">WORKSPACE</div>
      <button className="nav" onClick={onBack}><span className="nav-icon">▦</span>Overview</button>
      <button className={'nav ' + (page === 'billing' ? 'active' : '')} onClick={() => window.location.hash = 'billing'}><span className="nav-icon">▤</span>Billing & plans</button>
      <button className={'nav ' + (page === 'payments' ? 'active' : '')} onClick={() => window.location.hash = 'payments'}><span className="nav-icon">↔</span>Payments</button>
      <button className={'nav ' + (page === 'profile' ? 'active' : '')} onClick={() => window.location.hash = 'profile'}><span className="nav-icon">◉</span>My profile</button>
      <button className={'nav ' + (page === 'settings' ? 'active' : '')} onClick={() => window.location.hash = 'settings'}><span className="nav-icon">⚙</span>Settings</button>
      <button className={'nav ' + (page === 'support' ? 'active' : '')} onClick={() => window.location.hash = 'support'}><span className="nav-icon">?</span>Help & support</button>
      <div className="sidebar-bottom"><div className="user-chip"><div className="avatar">{(session.user.email || 'U').slice(0,1).toUpperCase()}</div><div className="user-meta"><strong>{session.user.email}</strong><span>Workspace account</span></div></div><button className="signout" onClick={onSignOut}>Sign out <span>↗</span></button></div>
    </aside>
    <main className="main-area"><header className="topbar"><div><p className="eyebrow">MOBIWAVE CNC / WORKSPACE</p><h1>{heading}</h1><p className="muted">{description}</p></div><button className="secondary" onClick={onBack}>← Back to overview</button></header>
      {error && <div className="notice"><strong>Action could not be completed</strong><span>{error}</span><button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}
      {notice && <div className="success-notice"><strong>Saved</strong><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {page === 'profile' && <div className="page-grid"><section className="table-panel"><div className="panel-head"><div><h2>Personal details</h2><p className="muted">These details are saved to your signed-in account.</p></div></div><form className="settings-form" onSubmit={saveProfile}><label>Full name<input value={fullName} onChange={e => setFullName(e.target.value)} maxLength={120} placeholder="Your full name" /></label><label>Phone number<input value={phone} onChange={e => setPhone(e.target.value)} maxLength={30} placeholder="+254…" /></label><label>Email address<input value={session.user.email || ''} readOnly /></label><button className="primary" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</button></form></section><section className="table-panel"><div className="panel-head"><div><h2>Change password</h2><p className="muted">Use a unique password for your account.</p></div></div><form className="settings-form" onSubmit={updatePassword}><label>New password<input name="newPassword" type="password" minLength="10" autoComplete="new-password" required placeholder="At least 10 characters" /></label><button className="primary" disabled={busy}>{busy ? 'Updating…' : 'Update password'}</button></form></section></div>}
      {page === 'billing' && <section className="table-panel"><div className="panel-head"><div><h2>Subscription overview</h2><p className="muted">Subscription and invoice data will appear here once a billing plan is assigned to this workspace.</p></div><span className="pill pending">Not configured</span></div><div className="empty"><strong>No subscription details available</strong><p>Contact MobiWave to confirm your plan, billing cycle, seat limits and renewal date.</p><a href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20billing%20enquiry">Contact billing support</a></div></section>}
      {page === 'payments' && <section className="table-panel"><div className="panel-head"><div><h2>Payment history</h2><p className="muted">Completed and pending payment records will be listed here when the billing gateway is connected.</p></div><span className="record-count">0 transactions</span></div><div className="table-scroll"><table><thead><tr><th>Date</th><th>Reference</th><th>Method</th><th>Amount</th><th>Status</th><th>Receipt</th></tr></thead><tbody><tr><td colSpan="6"><div className="empty"><strong>No payment records yet</strong><p>No transactions are currently available for this workspace. Do not make a payment until MobiWave issues an official invoice or payment instruction.</p></div></td></tr></tbody></table></div></section>}
      {page === 'settings' && <div className="page-grid"><section className="table-panel"><div className="panel-head"><div><h2>Workspace access</h2><p className="muted">Access is managed by MobiWave and your authorised organisation administrator.</p></div></div><div className="settings-list"><div><strong>Signed-in email</strong><span>{session.user.email}</span></div><div><strong>Account ID</strong><span>{session.user.id}</span></div><div><strong>Email verification</strong><span>{session.user.email_confirmed_at ? 'Confirmed' : 'Pending confirmation'}</span></div><div><strong>Multi-factor authentication</strong><span>Use account security options supported by your sign-in provider.</span></div></div></section><section className="table-panel"><div className="panel-head"><div><h2>Organisation changes</h2><p className="muted">For company name, branch, department or administrator changes, request an update from MobiWave.</p></div></div><div className="empty"><a href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20workspace%20change">Request a workspace change</a></div></section></div>}
      {page === 'support' && <section className="table-panel"><div className="panel-head"><div><h2>MobiWave support</h2><p className="muted">Tell us what you need help with and include your company name and a transaction or expense reference if relevant.</p></div></div><div className="settings-list"><div><strong>Email support</strong><a href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20support">info@mobiwave.co.ke</a></div><div><strong>Account access</strong><span>Request an invitation or ask your company administrator to contact us.</span></div><div><strong>Billing</strong><a href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20billing">Contact billing support</a></div></div></section>}
      <footer className="app-footer">MobiWave Innovations Ltd <span>·</span> MobiWave CNC <span>·</span> Workspace</footer>
    </main>
  </div>
}

function App() {
  const [session, setSession] = useState(null)
  const [showLogin, setShowLogin] = useState(window.location.pathname === '/login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [expenses, setExpenses] = useState([])
  const [filter, setFilter] = useState('all')
  const [search, setSearch] = useState('')
  const [lastUpdated, setLastUpdated] = useState(null)
  const [activePage, setActivePage] = useState(window.location.hash.replace('#', '') || 'overview')

  useEffect(() => {
    if (!supabase) { setLoading(false); return }
    supabase.auth.getSession().then(({ data, error: authError }) => {
      if (authError) setError(authError.message)
      setSession(data?.session ?? null)
      setLoading(false)
    })
    const { data: listener } = supabase.auth.onAuthStateChange((_event, nextSession) => setSession(nextSession))
    return () => listener.subscription.unsubscribe()
  }, [])

  useEffect(() => {
    const syncHash = () => setActivePage(window.location.hash.replace('#', '') || 'overview')
    window.addEventListener('hashchange', syncHash)
    return () => window.removeEventListener('hashchange', syncHash)
  }, [])

  useEffect(() => {
    const syncRoute = () => setShowLogin(window.location.pathname === '/login')
    window.addEventListener('popstate', syncRoute)
    return () => window.removeEventListener('popstate', syncRoute)
  }, [])

  function openLogin() {
    window.history.pushState({}, '', '/login')
    setShowLogin(true)
    window.scrollTo(0, 0)
  }

  const loadExpenses = useCallback(async () => {
    if (!supabase || !session) return
    setBusy(true)
    setError('')
    const { data, error: queryError } = await supabase
      .from('expenses')
      .select('id, merchant_name, gross_amount, category, status, verification_status, expense_date, created_at, currency')
      .order('created_at', { ascending: false })
      .limit(250)
    if (queryError) setError(queryError.message)
    else { setExpenses(data ?? []); setLastUpdated(new Date()) }
    setBusy(false)
  }, [session])

  useEffect(() => { loadExpenses() }, [loadExpenses])

  const visible = useMemo(() => expenses.filter((item) => {
    const matchesFilter = filter === 'all' || (filter === 'review' ? ['submitted', 'needs_review'].includes(item.status) || ['needs_review', 'verification_failed', 'awaiting_verification'].includes(item.verification_status) : item.status === filter)
    const q = search.trim().toLowerCase()
    const matchesSearch = !q || [item.merchant_name, item.category, item.status, item.verification_status, item.id].some(v => String(v ?? '').toLowerCase().includes(q))
    return matchesFilter && matchesSearch
  }), [expenses, filter, search])

  const stats = useMemo(() => ({
    total: expenses.length,
    review: expenses.filter(x => ['submitted', 'needs_review'].includes(x.status) || ['needs_review', 'verification_failed', 'awaiting_verification'].includes(x.verification_status)).length,
    accepted: expenses.filter(x => x.status === 'accepted' || x.status === 'reconciled').length,
    value: expenses.reduce((sum, x) => sum + Number(x.gross_amount || 0), 0),
  }), [expenses])

  async function signIn(event) {
    event.preventDefault()
    if (!supabase) return
    setBusy(true); setError('')
    const { error: authError } = await supabase.auth.signInWithPassword({ email: email.trim(), password })
    if (authError) setError(authError.message)
    setBusy(false)
  }

  async function signOut() {
    if (!supabase) return
    await supabase.auth.signOut()
    setExpenses([])
  }

  if (loading) return <main className="center"><div className="loader" /><p>Loading secure workspace…</p></main>

  if (!session && !showLogin) return <Landing onLogin={openLogin} />

  if (session && (session.user.app_metadata?.role === 'super_admin' || session.user.app_metadata?.is_super_admin === true)) {
    if (['billing', 'plans', 'payments', 'audit'].includes(activePage)) return <PlatformAdminPage page={activePage} supabase={supabase} session={session} onSignOut={signOut} onNavigate={(p) => { window.location.hash = p === 'overview' ? '' : p; setActivePage(p) }} />
    if (['profile', 'settings', 'support'].includes(activePage)) return <WorkspacePage page={activePage} session={session} supabase={supabase} onBack={() => { window.location.hash = ''; setActivePage('overview') }} onSignOut={signOut} />
    return <AdminDashboard supabase={supabase} session={session} onSignOut={signOut} />
  }

  if (!supabase) return <main className="center"><section className="setup-panel"><div className="brand-mark">M</div><p className="eyebrow">MOBIWAVE · FINANCE OPERATIONS</p><h1>Configuration required</h1><p>Set <code>VITE_SUPABASE_URL</code> and <code>VITE_SUPABASE_ANON_KEY</code> in the Vercel project environment variables, then redeploy.</p><p className="muted">Only the public/anon key belongs in the browser. Never add a service-role key here.</p></section></main>

  if (session && !['overview', ''].includes(activePage) && ['billing', 'payments', 'profile', 'settings', 'support'].includes(activePage)) return <WorkspacePage page={activePage} session={session} supabase={supabase} onBack={() => { window.location.hash = ''; setActivePage('overview') }} onSignOut={signOut} />

  if (!session) return <main className="auth-shell">
    <section className="auth-intro"><div className="brand-mark">M</div><p className="eyebrow">MOBIWAVE INNOVATIONS</p><h1>Expense &amp; eTIMS</h1><p className="intro-copy">Finance operations, invoice review and expense reconciliation in one secure workspace.</p><div className="auth-foot">Kenya · KES · Role-controlled access</div></section>
    <section className="auth-form-wrap"><form className="auth-form" onSubmit={signIn}><p className="eyebrow">SECURE SIGN IN</p><h2>Welcome back</h2><p className="muted">Use your authorised workspace account.</p><label>Email address<input autoComplete="username" type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@company.com" /></label><label>Password<input autoComplete="current-password" type="password" required value={password} onChange={e => setPassword(e.target.value)} placeholder="Enter password" /></label>{error && <p className="error">{error}</p>}<button className="primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in securely'}</button><p className="fine-print">Need an account for your company? <a href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20company%20access">Contact MobiWave to request access.</a></p><p className="fine-print"><a href="/" onClick={e => { e.preventDefault(); window.history.pushState({}, '', '/'); setShowLogin(false); window.scrollTo(0, 0) }}>Back to website</a></p></form></section>
  </main>

  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><div className="brand-mark small">M</div><div><strong>MobiWave</strong><span>Finance operations</span></div></div><div className="side-label">WORKSPACE</div><button className={'nav ' + (activePage === 'overview' ? 'active' : '')} onClick={() => { window.location.hash = ''; setActivePage('overview') }}><span className="nav-icon">▦</span>Overview</button><button className="nav" onClick={() => setFilter('review')}><span className="nav-icon">◷</span>Expense review <span className="nav-count">{stats.review}</span></button><button className="nav" onClick={() => setFilter('reconciled')}><span className="nav-icon">✓</span>Reconciled</button><button className={'nav ' + (activePage === 'billing' ? 'active' : '')} onClick={() => { window.location.hash = 'billing'; setActivePage('billing') }}><span className="nav-icon">▤</span>Billing & plans</button><button className={'nav ' + (activePage === 'payments' ? 'active' : '')} onClick={() => { window.location.hash = 'payments'; setActivePage('payments') }}><span className="nav-icon">↔</span>Payments</button><button className={'nav ' + (activePage === 'profile' ? 'active' : '')} onClick={() => { window.location.hash = 'profile'; setActivePage('profile') }}><span className="nav-icon">◉</span>My profile</button><button className={'nav ' + (activePage === 'settings' ? 'active' : '')} onClick={() => { window.location.hash = 'settings'; setActivePage('settings') }}><span className="nav-icon">⚙</span>Settings</button><button className={'nav ' + (activePage === 'support' ? 'active' : '')} onClick={() => { window.location.hash = 'support'; setActivePage('support') }}><span className="nav-icon">?</span>Help & support</button><div className="sidebar-bottom"><div className="user-chip"><div className="avatar">{(session.user.email || 'U').slice(0,1).toUpperCase()}</div><div className="user-meta"><strong>{session.user.email}</strong><span>Authenticated user</span></div></div><button className="signout" onClick={signOut}>Sign out <span>↗</span></button></div></aside>
    <main className="main-area"><header className="topbar"><div><p className="eyebrow">FINANCE / OVERVIEW</p><h1>Finance operations</h1><p className="muted">Review submitted expenses and track invoice verification.</p></div><button className="secondary" onClick={loadExpenses} disabled={busy}>{busy ? 'Refreshing…' : '↻ Refresh data'}</button></header>
      {error && <div className="notice"><strong>Could not load data</strong><span>{error}</span><button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}
      <section className="stat-grid"><article className="stat"><span className="stat-label">Expense records</span><strong>{stats.total}</strong><span className="stat-note">Latest 250 records</span></article><article className="stat"><span className="stat-label">Needs attention</span><strong>{stats.review}</strong><span className="stat-note warning-text">Review or verification pending</span></article><article className="stat"><span className="stat-label">Accepted / reconciled</span><strong>{stats.accepted}</strong><span className="stat-note">Completed review status</span></article><article className="stat"><span className="stat-label">Total listed value</span><strong className="amount">{money(stats.value)}</strong><span className="stat-note">Across loaded records</span></article></section>
      <section className="table-panel"><div className="panel-head"><div><h2>Expense register</h2><p className="muted">Data is limited to records visible to your account permissions.</p></div><span className="record-count">{visible.length} records</span></div><div className="table-tools"><div className="search-box"><span>⌕</span><input aria-label="Search expenses" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search merchant, category or reference" /></div><select value={filter} onChange={e => setFilter(e.target.value)} aria-label="Filter expenses"><option value="all">All expenses</option><option value="review">Needs attention</option><option value="submitted">Submitted</option><option value="needs_review">Needs review</option><option value="accepted">Accepted</option><option value="reconciled">Reconciled</option><option value="rejected">Rejected</option></select></div>
      <div className="table-scroll"><table><thead><tr><th>Merchant / reference</th><th>Category</th><th>Expense date</th><th>Amount</th><th>Status</th><th>Invoice check</th></tr></thead><tbody>{visible.map(item => <tr key={item.id}><td><strong>{item.merchant_name || 'Unnamed merchant'}</strong><small>{item.id.slice(0,8).toUpperCase()}</small></td><td className="capitalize">{item.category || '—'}</td><td>{date(item.expense_date)}</td><td className="amount">{money(item.gross_amount)}</td><td><span className={'pill ' + (item.status === 'accepted' || item.status === 'reconciled' ? 'good' : item.status === 'rejected' ? 'bad' : 'pending')}>{(item.status || 'unknown').replaceAll('_',' ')}</span></td><td><span className={'pill ' + (item.verification_status === 'externally_verified' ? 'good' : 'pending')}>{(item.verification_status || 'unknown').replaceAll('_',' ')}</span></td></tr>)}</tbody></table>{visible.length === 0 && <div className="empty"><div className="empty-symbol">⌕</div><strong>{expenses.length ? 'No matching expenses' : 'No expense records available'}</strong><p>{expenses.length ? 'Try changing your search or filter.' : 'Records will appear here when available to your account.'}</p></div>}</div>
      <footer className="table-footer"><span>{lastUpdated ? 'Last refreshed ' + lastUpdated.toLocaleTimeString('en-KE', {hour:'2-digit',minute:'2-digit'}) : 'Waiting for data'}</span><span>Read-only dashboard · Review actions remain in the authorised workflow</span></footer></section>
      <footer className="app-footer">MobiWave Innovations Ltd <span>·</span> Expense &amp; eTIMS <span>·</span> Secure finance workspace</footer>
    </main>
  </div>
}

createRoot(document.getElementById('root')).render(<App />)
