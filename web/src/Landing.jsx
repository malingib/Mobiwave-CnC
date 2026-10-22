import React from 'react'

const features = [
  { number: '01', title: 'Capture expenses', copy: 'Keep expense details, references and supporting documents together in one reviewable record.' },
  { number: '02', title: 'Check eTIMS invoices', copy: 'Track verification outcomes and make exceptions visible when a record needs a person to review it.' },
  { number: '03', title: 'Close the loop', copy: 'Follow records from submission through review and reconciliation without losing the context.' },
]

const workflow = [
  { number: '01', title: 'Capture', copy: 'Bring the expense record and its supporting evidence together.' },
  { number: '02', title: 'Verify', copy: 'Record invoice checks and make the outcome easy to understand.' },
  { number: '03', title: 'Review', copy: 'Send unclear or exceptional items for a manual decision.' },
  { number: '04', title: 'Reconcile', copy: 'Keep the final record and its status visible to authorised teams.' },
]

export default function Landing({ onLogin }) {
  return <div className="landing">
    <header className="landing-nav">
      <a className="landing-brand" href="#top" aria-label="MobiWave CNC home"><span className="landing-logo">M</span><span><strong>MobiWave</strong><small>CNC · Expense &amp; eTIMS</small></span></a>
      <nav className="landing-links" aria-label="Main navigation"><a href="#platform">Platform</a><a href="#workflow">Workflow</a><a href="#security">Access &amp; control</a></nav>
      <button className="landing-nav-cta" onClick={onLogin}>Sign in <span aria-hidden="true">↗</span></button>
    </header>
    <main id="top">
      <section className="landing-hero">
        <div className="landing-hero-copy">
          <p className="landing-kicker"><span /> FINANCE OPERATIONS, WITHOUT THE GUESSWORK</p>
          <h1>Know what was spent. <em>Verify what was invoiced.</em></h1>
          <p className="landing-lead">MobiWave CNC brings expense records, eTIMS invoice checks and reconciliation into one practical workspace — so your team can see what is complete, what needs review and what comes next.</p>
          <div className="landing-actions"><button className="landing-primary" onClick={onLogin}>Sign in to your workspace <span aria-hidden="true">→</span></button><a className="landing-text-link" href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20company%20access">Request company access <span aria-hidden="true">↗</span></a></div>
          <div className="landing-trust"><span className="trust-check">✓</span><span>Role-aware access</span><i /><span>KES-ready records</span><i /><span>Reviewable invoice status</span></div>
        </div>
        <div className="landing-preview" aria-label="Illustrative expense operations dashboard">
          <div className="preview-window">
            <div className="preview-top"><div className="preview-dots"><b/><b/><b/></div><span>FINANCE WORKSPACE</span><span className="preview-live"><i/> OVERVIEW</span></div>
            <div className="preview-title"><div><small>YOUR TEAM AT A GLANCE</small><h2>Expense register</h2></div><span className="preview-date">This month⌄</span></div>
            <div className="preview-metrics"><div><small>Records captured</small><strong>1,248</strong><span>Expense activity</span></div><div><small>Needs attention</small><strong className="metric-warn">36</strong><span>Review queue</span></div><div><small>Reconciled</small><strong>92%</strong><span>Completion view</span></div></div>
            <div className="preview-table-head"><span>MERCHANT / REFERENCE</span><span>AMOUNT</span><span>STATUS</span></div>
            <div className="preview-row"><span><b className="merchant-icon">K</b><span><strong>Kenya Airways</strong><small>TRV-00481</small></span></span><strong>KES 18,400</strong><label className="status-ok">Verified</label></div>
            <div className="preview-row"><span><b className="merchant-icon hotel">H</b><span><strong>Coastline Hotel</strong><small>TRV-00480</small></span></span><strong>KES 9,600</strong><label className="status-review">Review</label></div>
            <div className="preview-row"><span><b className="merchant-icon meal">M</b><span><strong>Market Bistro</strong><small>MEAL-00479</small></span></span><strong>KES 2,250</strong><label className="status-ok">Matched</label></div>
            <div className="preview-foot"><span><i/> Invoice checks and review status</span><span>Illustrative preview</span></div>
          </div>
          <div className="preview-note"><span className="note-icon">✓</span><span><strong>One connected workflow</strong><small>Capture · Verify · Review · Reconcile</small></span></div>
        </div>
        <div className="hero-bottom-rule"><span>BUILT FOR ACCOUNTABILITY</span><span>CAPTURE → VERIFY → REVIEW → RECONCILE</span></div>
      </section>
      <section className="landing-intro" id="platform"><div><p className="landing-kicker">THE CNC WORKSPACE</p><h2>Less chasing records.<br/><span>More clarity at every step.</span></h2></div><p>Keep expense details, invoice verification outcomes and review progress connected. MobiWave CNC helps authorised teams find exceptions sooner and understand the status of a record without treating a check as an approval.</p></section>
      <section className="landing-features">{features.map(feature => <article className="landing-feature" key={feature.number}><span className="feature-number">{feature.number}</span><div><h3>{feature.title}</h3><p>{feature.copy}</p></div><span className="feature-arrow" aria-hidden="true">↗</span></article>)}</section>
      <section className="landing-workflow" id="workflow"><div className="workflow-heading"><p className="landing-kicker">A TRACEABLE PROCESS</p><h2>From first record<br/>to final reconciliation.</h2><p>Make each stage visible, with a clear path for records that need attention.</p></div><div className="workflow-steps">{workflow.map(step => <article key={step.number}><span>{step.number}</span><div><h3>{step.title}</h3><p>{step.copy}</p></div></article>)}</div></section>
      <section className="landing-security" id="security"><div className="security-symbol"><span>✓</span></div><div><p className="landing-kicker">ACCESS WITH ACCOUNTABILITY</p><h2>Financial records need<br/>careful access controls.</h2><p>The workspace is designed around authenticated access and permission-aware records. Invoice checks and review exceptions remain distinct, so an inconclusive result is not presented as a confirmed outcome.</p></div><div className="security-points"><span><b>01</b> Authenticated sign-in</span><span><b>02</b> Permission-aware records</span><span><b>03</b> Visible review status</span></div></section>
      <section className="landing-cta"><div><p className="landing-kicker">YOUR WORKFLOW, MORE VISIBLE</p><h2>Bring expenses and invoice checks<br/>into one clear workspace.</h2><p>Already have an account? Sign in. Need access for your company? Contact the MobiWave team.</p></div><div className="landing-cta-actions"><button className="landing-primary" onClick={onLogin}>Company sign in <span aria-hidden="true">→</span></button><a className="landing-text-link" href="mailto:info@mobiwave.co.ke?subject=MobiWave%20CNC%20company%20access">Request access <span aria-hidden="true">↗</span></a></div></section>
    </main>
    <footer className="landing-footer"><a className="landing-brand" href="#top"><span className="landing-logo">M</span><span><strong>MobiWave</strong><small>CNC · Expense &amp; eTIMS</small></span></a><span>© {new Date().getFullYear()} MobiWave Innovations Ltd</span><span>Kenya · KES · Finance operations</span></footer>
  </div>
}
