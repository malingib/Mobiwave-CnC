import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { createClient } from '@supabase/supabase-js'
import './style.css'
import Landing from './Landing.jsx'
import OrganisationConfiguration from './OrganisationConfiguration.jsx'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseKey = import.meta.env.VITE_SUPABASE_ANON_KEY
const supabase = supabaseUrl && supabaseKey ? createClient(supabaseUrl, supabaseKey) : null
const money = (value) => new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES', maximumFractionDigits: 2 }).format(Number(value || 0))
const date = (value) => value ? new Date(value).toLocaleDateString('en-KE', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'


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

  // Platform administrators use the same provisioned Sun King workspace. Privileged actions
  // are exposed only through permission-checked organisation workflows and database policies.

  if (!supabase) return <main className="center"><section className="setup-panel"><div className="brand-mark">M</div><p className="eyebrow">MOBIWAVE · FINANCE OPERATIONS</p><h1>Configuration required</h1><p>Set <code>VITE_SUPABASE_URL</code> and <code>VITE_SUPABASE_ANON_KEY</code> in the Vercel project environment variables, then redeploy.</p><p className="muted">Only the public/anon key belongs in the browser. Never add a service-role key here.</p></section></main>

  if (session && activePage === 'organisation') return <OrganisationConfiguration supabase={supabase} session={session} onBack={() => { window.location.hash = ''; setActivePage('overview') }} onSignOut={signOut} />
  if (session && !['overview', ''].includes(activePage) && ['billing', 'payments', 'profile', 'settings', 'support'].includes(activePage)) return <WorkspacePage page={activePage} session={session} supabase={supabase} onBack={() => { window.location.hash = ''; setActivePage('overview') }} onSignOut={signOut} />

  if (!session) return <main className="auth-shell">
    <section className="auth-intro"><div className="brand-mark">M</div><p className="eyebrow">MOBIWAVE INNOVATIONS</p><h1>Expense &amp; eTIMS</h1><p className="intro-copy">Finance operations, invoice review and expense reconciliation in one secure workspace.</p><div className="auth-foot">Kenya · KES · Role-controlled access</div></section>
    <section className="auth-form-wrap"><form className="auth-form" onSubmit={signIn}><p className="eyebrow">SECURE SIGN IN</p><h2>Welcome back</h2><p className="muted">Use your authorised workspace account.</p><label>Email address<input autoComplete="username" type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@company.com" /></label><label>Password<input autoComplete="current-password" type="password" required value={password} onChange={e => setPassword(e.target.value)} placeholder="Enter password" /></label>{error && <p className="error">{error}</p>}<button className="primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in securely'}</button><p className="fine-print">Need an account for your company? <a href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20company%20access">Contact MobiWave to request access.</a></p><p className="fine-print"><a href="/" onClick={e => { e.preventDefault(); window.history.pushState({}, '', '/'); setShowLogin(false); window.scrollTo(0, 0) }}>Back to website</a></p></form></section>
  </main>

  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><div className="brand-mark small">M</div><div><strong>MobiWave</strong><span>Finance operations</span></div></div><div className="side-label">WORKSPACE</div><button className={'nav ' + (activePage === 'overview' ? 'active' : '')} onClick={() => { window.location.hash = ''; setActivePage('overview') }}><span className="nav-icon">▦</span>Overview</button><button className="nav" onClick={() => setFilter('review')}><span className="nav-icon">◷</span>Expense review <span className="nav-count">{stats.review}</span></button><button className="nav" onClick={() => setFilter('reconciled')}><span className="nav-icon">✓</span>Reconciled</button><button className={'nav ' + (activePage === 'billing' ? 'active' : '')} onClick={() => { window.location.hash = 'billing'; setActivePage('billing') }}><span className="nav-icon">▤</span>Billing & plans</button><button className={'nav ' + (activePage === 'payments' ? 'active' : '')} onClick={() => { window.location.hash = 'payments'; setActivePage('payments') }}><span className="nav-icon">↔</span>Payments</button><button className={'nav ' + (activePage === 'profile' ? 'active' : '')} onClick={() => { window.location.hash = 'profile'; setActivePage('profile') }}><span className="nav-icon">◉</span>My profile</button><button className={'nav ' + (activePage === 'settings' ? 'active' : '')} onClick={() => { window.location.hash = 'settings'; setActivePage('settings') }}><span className="nav-icon">⚙</span>Settings</button><button className={'nav ' + (activePage === 'organisation' ? 'active' : '')} onClick={() => { window.location.hash = 'organisation'; setActivePage('organisation') }}><span className="nav-icon">⌘</span>Organisation structure</button><button className={'nav ' + (activePage === 'support' ? 'active' : '')} onClick={() => { window.location.hash = 'support'; setActivePage('support') }}><span className="nav-icon">?</span>Help & support</button><div className="sidebar-bottom"><div className="user-chip"><div className="avatar">{(session.user.email || 'U').slice(0,1).toUpperCase()}</div><div className="user-meta"><strong>{session.user.email}</strong><span>Authenticated user</span></div></div><button className="signout" onClick={signOut}>Sign out <span>↗</span></button></div></aside>
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
