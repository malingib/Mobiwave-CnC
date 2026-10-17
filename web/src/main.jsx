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

  if (!supabase) return <main className="center"><section className="setup-panel"><div className="brand-mark">M</div><p className="eyebrow">MOBIWAVE · FINANCE OPERATIONS</p><h1>Configuration required</h1><p>Set <code>VITE_SUPABASE_URL</code> and <code>VITE_SUPABASE_ANON_KEY</code> in the Vercel project environment variables, then redeploy.</p><p className="muted">Only the public/anon key belongs in the browser. Never add a service-role key here.</p></section></main>

  if (!session) return <main className="auth-shell">
    <section className="auth-intro"><div className="brand-mark">M</div><p className="eyebrow">MOBIWAVE INNOVATIONS</p><h1>Expense &amp; eTIMS</h1><p className="intro-copy">Finance operations, invoice review and expense reconciliation in one secure workspace.</p><div className="auth-foot">Kenya · KES · Role-controlled access</div></section>
    <section className="auth-form-wrap"><form className="auth-form" onSubmit={signIn}><p className="eyebrow">SECURE SIGN IN</p><h2>Welcome back</h2><p className="muted">Use your authorised workspace account.</p><label>Email address<input autoComplete="username" type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@company.com" /></label><label>Password<input autoComplete="current-password" type="password" required value={password} onChange={e => setPassword(e.target.value)} placeholder="Enter password" /></label>{error && <p className="error">{error}</p>}<button className="primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in securely'}</button><p className="fine-print">Access is enforced by Supabase Auth and database row-level security.</p></form></section>
  </main>

  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><div className="brand-mark small">M</div><div><strong>MobiWave</strong><span>Finance operations</span></div></div><div className="side-label">WORKSPACE</div><button className="nav active"><span className="nav-icon">▦</span>Overview</button><button className="nav" onClick={() => setFilter('review')}><span className="nav-icon">◷</span>Expense review <span className="nav-count">{stats.review}</span></button><button className="nav" onClick={() => setFilter('reconciled')}><span className="nav-icon">✓</span>Reconciled</button><div className="sidebar-bottom"><div className="user-chip"><div className="avatar">{(session.user.email || 'U').slice(0,1).toUpperCase()}</div><div className="user-meta"><strong>{session.user.email}</strong><span>Authenticated user</span></div></div><button className="signout" onClick={signOut}>Sign out <span>↗</span></button></div></aside>
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
