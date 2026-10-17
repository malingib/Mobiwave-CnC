import React from 'react'

const features = [
  { number: '01', title: 'Expense visibility', copy: 'Bring employee expenses, receipts and supporting details into one organised register.' },
  { number: '02', title: 'eTIMS invoice checks', copy: 'Track invoice verification outcomes and route exceptions for manual review when needed.' },
  { number: '03', title: 'Review-ready records', copy: 'See what needs attention, what has been accepted and what has been reconciled.' },
]

export default function Landing({ onLogin }) {
  return <div className="landing">
    <header className="landing-nav">
      <a className="landing-brand" href="#top" aria-label="MobiWave CNC home"><span className="landing-logo">M</span><span><strong>MobiWave</strong><small>CNC · Expense &amp; eTIMS</small></span></a>
      <nav className="landing-links" aria-label="Main navigation"><a href="#platform">Platform</a><a href="#workflow">How it works</a><a href="#security">Security</a></nav>
      <button className="landing-nav-cta" onClick={onLogin}>Sign in <span aria-hidden="true">↗</span></button>
    </header>
    <main id="top">
      <section className="landing-hero">
        <div className="landing-hero-copy">
          <p className="landing-kicker"><span /> FINANCE OPERATIONS, MADE CLEAR</p>
          <h1>Expense control that keeps your records <em>moving.</em></h1>
          <p className="landing-lead">A practical workspace for expense tracking, eTIMS invoice verification and reconciliation — built to help teams turn scattered paperwork into review-ready records.</p>
          <div className="landing-actions"><button className="landing-primary" onClick={onLogin}>Sign in to your workspace <span aria-hidden="true">→</span></button><a className="landing-text-link" href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20company%20access">Request company access <span aria-hidden="true">↗</span></a></div>
          <div className="landing-trust"><span className="trust-check">✓</span><span>Role-controlled access</span><i /><span>KES-ready reporting</span><i /><span>Invoice review tracking</span></div>
        </div>
        <div className="landing-preview" aria-label="Illustration of the expense operations dashboard">
          <div className="preview-window">
            <div className="preview-top"><div className="preview-dots"><b/><b/><b/></div><span>FINANCE OVERVIEW</span><span className="preview-live"><i/> WORKSPACE</span></div>
            <div className="preview-title"><div><small>MONITOR YOUR OPERATIONS</small><h2>Expense register</h2></div><span className="preview-date">This month⌄</span></div>
            <div className="preview-metrics"><div><small>Records captured</small><strong>1,248</strong><span>Expense activity</span></div><div><small>Needs attention</small><strong className="metric-warn">36</strong><span>Review queue</span></div><div><small>Reconciled</small><strong>92%</strong><span>Completion view</span></div></div>
            <div className="preview-table-head"><span>MERCHANT / REFERENCE</span><span>AMOUNT</span><span>STATUS</span></div>
            <div className="preview-row"><span><b className="merchant-icon">K</b><span><strong>Kenya Airways</strong><small>TRV-00481</small></span></span><strong>KES 18,400</strong><label className="status-ok">Verified</label></div>
            <div className="preview-row"><span><b className="merchant-icon hotel">H</b><span><strong>Coastline Hotel</strong><small>TRV-00480</small></span></span><strong>KES 9,600</strong><label className="status-review">Review</label></div>
            <div className="preview-row"><span><b className="merchant-icon meal">M</b><span><strong>Market Bistro</strong><small>MEAL-00479</small></span></span><strong>KES 2,250</strong><label className="status-ok">Matched</label></div>
            <div className="preview-foot"><span><i/> Invoice checks and review status</span><span>Illustrative preview</span></div>
          </div>
          <div className="preview-note"><span className="note-icon">✓</span><span><strong>One clear view</strong><small>Expenses, checks and next steps</small></span></div>
        </div>
        <div className="hero-bottom-rule"><span>BUILT FOR ACCOUNTABILITY</span><span>CAPTURE → VERIFY → REVIEW → RECONCILE</span></div>
      </section>
      <section className="landing-intro" id="platform"><div><p className="landing-kicker">THE PLATFORM</p><h2>Less chasing paperwork.<br/><span>More control over the details.</span></h2></div><p>From employee expense submissions to invoice verification outcomes, MobiWave CNC gives authorised teams a shared place to inspect records, spot exceptions and follow progress.</p></section>
      <section className="landing-features">{features.map(feature => <article className="landing-feature" key={feature.number}><span className="feature-number">{feature.number}</span><div><h3>{feature.title}</h3><p>{feature.copy}</p></div><span className="feature-arrow" aria-hidden="true">↗</span></article>)}</section>
      <section className="landing-workflow" id="workflow"><div className="workflow-heading"><p className="landing-kicker">A CLEARER PROCESS</p><h2>From expense record<br/>to confident review.</h2><p>Keep the work visible at every stage without confusing a verification result with a final approval.</p></div><div className="workflow-steps"><article><span>01</span><div><h3>Capture</h3><p>Keep expense details and invoice evidence together for review.</p></div></article><article><span>02</span><div><h3>Verify</h3><p>Track invoice checks and flag items that need a closer look.</p></div></article><article><span>03</span><div><h3>Resolve</h3><p>Use a manual-review path when automated verification is unavailable or inconclusive.</p></div></article><article><span>04</span><div><h3>Reconcile</h3><p>Follow status through to accepted or reconciled records.</p></div></article></div></section>
      <section className="landing-security" id="security"><div className="security-symbol"><span>✓</span></div><div><p className="landing-kicker">ACCESS WITH ACCOUNTABILITY</p><h2>Business records deserve<br/>careful access controls.</h2><p>The workspace uses authenticated sign-in and database-level access policies. Users only see records permitted by their account. Verification exceptions remain visible for review rather than being presented as confirmed results.</p></div><div className="security-points"><span><b>01</b> Authenticated access</span><span><b>02</b> Permission-aware records</span><span><b>03</b> Clear verification status</span></div></section>
      <section className="landing-cta"><div><p className="landing-kicker">READY WHEN YOU ARE</p><h2>Bring your expense workflow<br/>into one clear workspace.</h2><p>Already have an account? Sign in. New to MobiWave CNC? Request access for your company.</p></div><div className="landing-cta-actions"><button className="landing-primary" onClick={onLogin}>Company sign in <span aria-hidden="true">→</span></button><a className="landing-text-link" href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20company%20access">Request access <span aria-hidden="true">↗</span></a></div></section>
    </main>
    <footer className="landing-footer"><a className="landing-brand" href="#top"><span className="landing-logo">M</span><span><strong>MobiWave</strong><small>CNC · Expense &amp; eTIMS</small></span></a><span>© {new Date().getFullYear()} MobiWave Innovations Ltd</span><span>Kenya · KES · Finance operations</span></footer>
  </div>
}
