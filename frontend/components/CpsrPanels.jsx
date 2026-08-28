'use client';

import { useCallback, useEffect, useState } from 'react';
import { ApiError, evaluationsApi, llmApi, toxicityApi } from '@/lib/api';
import { Modal } from '@/components/Modal';
import { useToast } from '@/components/Toast';

const EMPTY_SUBSTANCE = {
  substanceName: '', casNumber: '', ecNumber: '', molecularFormula: '',
  molecularWeight: '', purity: '', intendedUse: '', intendedConcentration: '',
  productType: '', targetPopulation: '', remarks: '',
};

function numberOrNull(value) {
  if (value === '' || value === null || value === undefined) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function substancePayload(form) {
  return {
    substanceName: form.substanceName,
    casNumber: form.casNumber || null,
    ecNumber: form.ecNumber || null,
    molecularFormula: form.molecularFormula || null,
    molecularWeight: numberOrNull(form.molecularWeight),
    purity: form.purity || null,
    intendedUse: form.intendedUse || null,
    intendedConcentration: numberOrNull(form.intendedConcentration),
    productType: form.productType || null,
    targetPopulation: form.targetPopulation || null,
    remarks: form.remarks || null,
  };
}

export function SubstancesPanel({ requestId }) {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(EMPTY_SUBSTANCE);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [toxicityHits, setToxicityHits] = useState(null);
  const [pubChemData, setPubChemData] = useState(null);
  const [pubChemLoading, setPubChemLoading] = useState(false);

  const load = useCallback(async () => {
    try {
      setItems(await evaluationsApi.substances(requestId));
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load substances.');
    } finally {
      setLoading(false);
    }
  }, [requestId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function searchLocalToxicity() {
    if (!form.substanceName.trim() && !form.casNumber.trim()) {
      toast.error('Enter a substance name or CAS number first.');
      return;
    }
    try {
      const params = {};
      if (form.casNumber.trim()) params.casNumber = form.casNumber.trim();
      else params.substanceName = form.substanceName.trim();
      const hits = await toxicityApi.search(params);
      setToxicityHits(hits);
      setPubChemData(null);
      if (hits.length > 0) toast.success(`Found ${hits.length} local toxicity record(s).`);
      else toast.info('No local records found. Try PubChem live lookup.');
    } catch (caught) {
      toast.error(caught instanceof ApiError ? caught.message : 'Toxicity search failed.');
    }
  }

  async function livePubChemLookup() {
    if (!form.substanceName.trim() && !form.casNumber.trim()) {
      toast.error('Enter a substance name or CAS number for live PubChem lookup.');
      return;
    }
    setPubChemLoading(true);
    setPubChemData(null);
    try {
      const params = {};
      if (form.casNumber.trim()) params.casNumber = form.casNumber.trim();
      if (form.substanceName.trim()) params.substanceName = form.substanceName.trim();
      const res = await toxicityApi.lookup(params);
      setPubChemData(res);
      setToxicityHits(null);
      toast.success(`PubChem lookup complete for ${res.resolvedName || 'substance'}`);
    } catch (caught) {
      toast.error(caught instanceof ApiError ? caught.message : 'PubChem lookup failed.');
    } finally {
      setPubChemLoading(false);
    }
  }

  function applyPubChemData() {
    if (!pubChemData) return;
    setForm((prev) => ({
      ...prev,
      substanceName: prev.substanceName || pubChemData.resolvedName || '',
      casNumber: prev.casNumber || pubChemData.casNumber || '',
      molecularFormula: pubChemData.molecularFormula || prev.molecularFormula,
      molecularWeight: pubChemData.molecularWeight != null ? pubChemData.molecularWeight : prev.molecularWeight,
      remarks: [
        prev.remarks,
        pubChemData.ghsHazardCodes?.length ? `GHS: ${pubChemData.ghsHazardCodes.join(', ')}` : '',
        pubChemData.ghsSignalWord ? `Signal: ${pubChemData.ghsSignalWord}` : '',
      ].filter(Boolean).join(' | '),
    }));
    toast.success('PubChem formula & hazard data applied to form!');
  }

  async function save(event) {
    event.preventDefault();
    if (!form.substanceName.trim()) {
      setError('Substance name is required.');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      if (editingId === null) await evaluationsApi.addSubstance(requestId, substancePayload(form));
      else await evaluationsApi.updateSubstance(requestId, editingId, substancePayload(form));
      setForm(EMPTY_SUBSTANCE);
      setEditingId(null);
      setToxicityHits(null);
      setPubChemData(null);
      toast.success(editingId === null ? 'Substance added.' : 'Substance updated.');
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to save substance.');
      toast.error(caught instanceof ApiError ? caught.message : 'Save failed.');
    } finally {
      setSaving(false);
    }
  }

  async function remove(item) {
    if (!window.confirm(`Remove substance "${item.substanceName}"?`)) return;
    try {
      await evaluationsApi.deleteSubstance(requestId, item.id);
      toast.success('Substance removed.');
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to delete substance.');
    }
  }

  function edit(item) {
    setEditingId(item.id);
    setForm({
      substanceName: item.substanceName || '',
      casNumber: item.casNumber || '',
      ecNumber: item.ecNumber || '',
      molecularFormula: item.molecularFormula || '',
      molecularWeight: item.molecularWeight ?? '',
      purity: item.purity || '',
      intendedUse: item.intendedUse || '',
      intendedConcentration: item.intendedConcentration ?? '',
      productType: item.productType || '',
      targetPopulation: item.targetPopulation || '',
      remarks: item.remarks || '',
    });
  }

  return (
    <div className="panel subpanel">
      <div className="panel-header compact">
        <div>
          <p className="panel-eyebrow">CPSR formulation</p>
          <h3>Chemical substances</h3>
        </div>
      </div>
      <form className="mini-form" onSubmit={save}>
        <div className="grid-2">
          <input
            required
            placeholder="Substance name *"
            value={form.substanceName}
            onChange={(e) => setForm({ ...form, substanceName: e.target.value })}
          />
          <input
            placeholder="CAS number (e.g. 50-00-0)"
            value={form.casNumber}
            onChange={(e) => setForm({ ...form, casNumber: e.target.value })}
          />
          <input
            placeholder="EC / EINECS number"
            value={form.ecNumber}
            onChange={(e) => setForm({ ...form, ecNumber: e.target.value })}
          />
          <input
            placeholder="Molecular formula (e.g. C7H6O2)"
            value={form.molecularFormula}
            onChange={(e) => setForm({ ...form, molecularFormula: e.target.value })}
          />
          <input
            type="number"
            step="any"
            placeholder="Molecular weight (g/mol)"
            value={form.molecularWeight}
            onChange={(e) => setForm({ ...form, molecularWeight: e.target.value })}
          />
          <input
            placeholder="Purity (%)"
            value={form.purity}
            onChange={(e) => setForm({ ...form, purity: e.target.value })}
          />
          <input
            placeholder="Intended use (e.g. Preservative)"
            value={form.intendedUse}
            onChange={(e) => setForm({ ...form, intendedUse: e.target.value })}
          />
          <input
            type="number"
            step="any"
            placeholder="Intended concentration (%)"
            value={form.intendedConcentration}
            onChange={(e) => setForm({ ...form, intendedConcentration: e.target.value })}
          />
          <input
            placeholder="Product type (e.g. Face cream, Rinse-off)"
            value={form.productType}
            onChange={(e) => setForm({ ...form, productType: e.target.value })}
          />
          <input
            placeholder="Target population (e.g. Adults, Children)"
            value={form.targetPopulation}
            onChange={(e) => setForm({ ...form, targetPopulation: e.target.value })}
          />
        </div>
        <textarea
          style={{ minHeight: 60 }}
          placeholder="Toxicological remarks / regulatory restrictions..."
          value={form.remarks}
          onChange={(e) => setForm({ ...form, remarks: e.target.value })}
        />
        <div className="action-row" style={{ flexWrap: 'wrap', gap: 8 }}>
          <button className="btn btn-primary small" disabled={saving}>
            {editingId === null ? 'Add substance' : 'Save substance'}
          </button>
          <button
            type="button"
            className="btn btn-secondary small"
            disabled={saving || pubChemLoading}
            onClick={() => void livePubChemLookup()}
          >
            {pubChemLoading ? 'Querying PubChem...' : '🌐 PubChem Live Lookup'}
          </button>
          <button
            type="button"
            className="btn btn-ghost small"
            disabled={saving}
            onClick={() => void searchLocalToxicity()}
          >
            Local DB Check
          </button>
          {editingId !== null && (
            <button
              type="button"
              className="btn btn-ghost small"
              onClick={() => {
                setEditingId(null);
                setForm(EMPTY_SUBSTANCE);
              }}
            >
              Cancel edit
            </button>
          )}
        </div>
      </form>

      {error && <div className="error-box" role="alert">{error}</div>}

      {/* PubChem live hazard card */}
      {pubChemData && (
        <div className="calc-card" style={{ borderColor: 'rgba(110, 168, 254, 0.4)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <strong>PubChem Result: {pubChemData.resolvedName || form.substanceName}</strong>
            <button type="button" className="btn btn-primary small" onClick={applyPubChemData}>
              Auto-fill Form
            </button>
          </div>
          <div style={{ fontSize: 13, display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 6 }}>
            <div><strong>Formula:</strong> {pubChemData.molecularFormula || '—'}</div>
            <div><strong>Mol. Weight:</strong> {pubChemData.molecularWeight ? `${pubChemData.molecularWeight} g/mol` : '—'}</div>
            <div><strong>Signal Word:</strong> <span style={{ color: '#f87171', fontWeight: 700 }}>{pubChemData.ghsSignalWord || 'None'}</span></div>
          </div>
          {pubChemData.ghsHazardStatements?.length > 0 && (
            <div style={{ marginTop: 8, fontSize: 12, color: 'var(--muted)' }}>
              <strong>Hazard Statements:</strong>
              <ul style={{ margin: '4px 0 0', paddingLeft: 16 }}>
                {pubChemData.ghsHazardStatements.slice(0, 4).map((h, i) => (
                  <li key={i}>{h}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Local DB hits */}
      {toxicityHits !== null && (
        toxicityHits.length === 0 ? (
          <div className="success-box" role="status">No local toxicity records found. Try live PubChem lookup.</div>
        ) : (
          <ul className="tiny-list" style={{ marginTop: 10 }}>
            {toxicityHits.map((hit) => (
              <li key={hit.id}>
                <strong>{hit.substanceName}</strong>
                <span> · CAS {hit.casNumber ?? '—'} · {hit.endpointName}: {hit.endpointValue} {hit.endpointUnit}</span>
                <small className="table-subtitle">{hit.sourceDb}{hit.testGuideline ? ` · ${hit.testGuideline}` : ''}</small>
              </li>
            ))}
          </ul>
        )
      )}

      {/* Existing Substances List */}
      {loading ? (
        <div className="empty-box small-pad">Loading substances...</div>
      ) : items.length === 0 ? (
        <div className="empty-box small-pad">No substances added to this formulation yet.</div>
      ) : (
        <ul className="tiny-list" style={{ marginTop: 14 }}>
          {items.map((item) => (
            <li key={item.id}>
              <strong>{item.substanceName}</strong>
              <span>
                {item.casNumber ? ` · CAS ${item.casNumber}` : ''}
                {item.molecularFormula ? ` · ${item.molecularFormula}` : ''}
                {item.intendedConcentration ? ` · ${item.intendedConcentration}%` : ''}
                {item.productType ? ` · ${item.productType}` : ''}
              </span>
              <span className="icon-actions">
                <button type="button" className="icon-button" onClick={() => edit(item)}>
                  Edit
                </button>
                <button type="button" className="icon-button" onClick={() => void remove(item)}>
                  Delete
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

const EMPTY_EVALUATION = {
  evaluatorId: '', sedValue: '', sedUnit: 'mg/kg/day', mosValue: '',
  noaelValue: '', noaelUnit: 'mg/kg/day', riskAssessment: '', conclusion: '',
  evaluatorOpinion: '', remarks: '',
};

function evaluationPayload(form) {
  return {
    evaluatorId: form.evaluatorId === '' ? null : Number(form.evaluatorId),
    sedValue: numberOrNull(form.sedValue),
    sedUnit: form.sedUnit || null,
    mosValue: numberOrNull(form.mosValue),
    noaelValue: numberOrNull(form.noaelValue),
    noaelUnit: form.noaelUnit || null,
    riskAssessment: form.riskAssessment || null,
    conclusion: form.conclusion || null,
    evaluatorOpinion: form.evaluatorOpinion || null,
    remarks: form.remarks || null,
  };
}

export function EvaluationsPanel({ requestId, users }) {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(EMPTY_EVALUATION);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // SCCS Calculator Modal State
  const [calcModalOpen, setCalcModalOpen] = useState(false);
  const [calcForm, setCalcForm] = useState({
    dailyAmountGrams: 1.5,
    concentrationPercent: 1.0,
    retentionFactor: 1.0,
    dermalAbsorptionPercent: 50.0,
    noaelMgKgDay: 100.0,
    bodyWeightKg: 60.0,
  });
  const [calcResult, setCalcResult] = useState(null);
  const [calcBusy, setCalcBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setItems(await evaluationsApi.evaluations(requestId));
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load evaluations.');
    } finally {
      setLoading(false);
    }
  }, [requestId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function runSedMosCalculator(e) {
    if (e) e.preventDefault();
    setCalcBusy(true);
    try {
      const res = await evaluationsApi.calculateSedMos(requestId, {
        dailyAmountGrams: Number(calcForm.dailyAmountGrams),
        concentrationPercent: Number(calcForm.concentrationPercent),
        retentionFactor: Number(calcForm.retentionFactor),
        dermalAbsorptionPercent: Number(calcForm.dermalAbsorptionPercent),
        noaelMgKgDay: Number(calcForm.noaelMgKgDay),
        bodyWeightKg: Number(calcForm.bodyWeightKg),
      });
      setCalcResult(res);
      toast.success('SCCS SED & MoS calculated!');
    } catch (caught) {
      toast.error(caught instanceof ApiError ? caught.message : 'Calculation failed.');
    } finally {
      setCalcBusy(false);
    }
  }

  function applyCalculatedValues() {
    if (!calcResult) return;
    setForm((prev) => ({
      ...prev,
      sedValue: calcResult.sedValue,
      sedUnit: calcResult.sedUnit || 'mg/kg bw/day',
      mosValue: calcResult.mosValue,
      noaelValue: calcForm.noaelMgKgDay,
      noaelUnit: 'mg/kg bw/day',
      conclusion: calcResult.conclusion,
      riskAssessment: `SCCS Evaluation: SED=${calcResult.sedValue} mg/kg bw/day, MoS=${calcResult.mosValue} (${calcResult.safe ? 'SAFE' : 'UNSAFE'}). ${calcResult.formulaBreakdown || ''}`,
    }));
    setCalcModalOpen(false);
    toast.success('Applied SCCS calculation to evaluation form!');
  }

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editingId === null) await evaluationsApi.createEvaluation(requestId, evaluationPayload(form));
      else await evaluationsApi.updateEvaluation(requestId, editingId, evaluationPayload(form));
      setForm(EMPTY_EVALUATION);
      setEditingId(null);
      toast.success(editingId === null ? 'Evaluation created.' : 'Evaluation updated.');
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to save evaluation.');
    } finally {
      setSaving(false);
    }
  }

  async function act(item, action) {
    setSaving(true);
    try {
      await evaluationsApi.evaluationAction(requestId, item.id, action);
      toast.success(`Evaluation ${action} completed.`);
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Operation failed.');
    } finally {
      setSaving(false);
    }
  }

  function edit(item) {
    setEditingId(item.id);
    setForm({
      evaluatorId: item.evaluatorId ?? '',
      sedValue: item.sedValue ?? '',
      sedUnit: item.sedUnit || 'mg/kg/day',
      mosValue: item.mosValue ?? '',
      noaelValue: item.noaelValue ?? '',
      noaelUnit: item.noaelUnit || 'mg/kg/day',
      riskAssessment: item.riskAssessment || '',
      conclusion: item.conclusion || '',
      evaluatorOpinion: item.evaluatorOpinion || '',
      remarks: item.remarks || '',
    });
  }

  return (
    <div className="panel subpanel">
      <div className="panel-header compact">
        <div>
          <p className="panel-eyebrow">Toxicology assessment</p>
          <h3>Risk evaluations & MoS</h3>
        </div>
        <button
          type="button"
          className="btn btn-secondary small"
          onClick={() => {
            setCalcModalOpen(true);
            if (!calcResult) void runSedMosCalculator();
          }}
        >
          🧮 SCCS SED / MoS Calculator
        </button>
      </div>

      <form className="mini-form" onSubmit={save}>
        <div className="grid-2">
          <select value={form.evaluatorId} onChange={(e) => setForm({ ...form, evaluatorId: e.target.value })}>
            <option value="">Evaluator (unassigned)</option>
            {(users || []).map((user) => (
              <option key={user.id} value={user.id}>
                {user.username} ({user.fullName})
              </option>
            ))}
          </select>
          <input
            type="number"
            step="any"
            placeholder="SED value (mg/kg bw/day)"
            value={form.sedValue}
            onChange={(e) => setForm({ ...form, sedValue: e.target.value })}
          />
          <input
            placeholder="SED unit"
            value={form.sedUnit}
            onChange={(e) => setForm({ ...form, sedUnit: e.target.value })}
          />
          <input
            type="number"
            step="any"
            placeholder="Margin of Safety (MoS >= 100 is Safe)"
            value={form.mosValue}
            onChange={(e) => setForm({ ...form, mosValue: e.target.value })}
          />
          <input
            type="number"
            step="any"
            placeholder="NOAEL value (mg/kg bw/day)"
            value={form.noaelValue}
            onChange={(e) => setForm({ ...form, noaelValue: e.target.value })}
          />
          <input
            placeholder="NOAEL unit"
            value={form.noaelUnit}
            onChange={(e) => setForm({ ...form, noaelUnit: e.target.value })}
          />
        </div>
        <textarea
          style={{ minHeight: 50 }}
          placeholder="Risk assessment summary"
          value={form.riskAssessment}
          onChange={(e) => setForm({ ...form, riskAssessment: e.target.value })}
        />
        <textarea
          style={{ minHeight: 50 }}
          placeholder="Conclusion (e.g. SAFE under specified conditions)"
          value={form.conclusion}
          onChange={(e) => setForm({ ...form, conclusion: e.target.value })}
        />
        <textarea
          style={{ minHeight: 50 }}
          placeholder="Evaluator opinion"
          value={form.evaluatorOpinion}
          onChange={(e) => setForm({ ...form, evaluatorOpinion: e.target.value })}
        />
        <div className="action-row">
          <button className="btn btn-primary small" disabled={saving}>
            {editingId === null ? 'Create evaluation' : 'Save evaluation'}
          </button>
          {editingId !== null && (
            <button
              type="button"
              className="btn btn-ghost small"
              onClick={() => {
                setEditingId(null);
                setForm(EMPTY_EVALUATION);
              }}
            >
              Cancel
            </button>
          )}
        </div>
      </form>

      {error && <div className="error-box" role="alert">{error}</div>}

      {loading ? (
        <div className="empty-box small-pad">Loading evaluations...</div>
      ) : items.length === 0 ? (
        <div className="empty-box small-pad">No evaluations yet. Use calculator or form above.</div>
      ) : (
        <ul className="tiny-list" style={{ marginTop: 14 }}>
          {items.map((item) => (
            <li key={item.id}>
              <span className="role-pill">{item.status}</span>
              {' '}evaluation #{item.id}
              {item.evaluatorName != null && <span> · evaluator {item.evaluatorName}</span>}
              {item.mosValue != null && (
                <div style={{ marginTop: 4 }}>
                  <strong style={{ color: item.mosValue >= 100 ? '#34d399' : '#f87171' }}>
                    MoS: {item.mosValue} ({item.mosValue >= 100 ? 'SAFE' : 'UNSAFE'})
                  </strong>
                  {item.sedValue != null && <span> · SED: {item.sedValue} {item.sedUnit || 'mg/kg/day'}</span>}
                  {item.conclusion && <p style={{ margin: '4px 0 0', color: 'var(--muted)', fontSize: 13 }}>{item.conclusion}</p>}
                </div>
              )}
              <span className="icon-actions" style={{ marginTop: 6 }}>
                {['PENDING', 'IN_PROGRESS'].includes(item.status) && (
                  <button type="button" className="icon-button" onClick={() => void edit(item)}>
                    Edit
                  </button>
                )}
                {item.status === 'PENDING' && (
                  <button type="button" className="icon-button" onClick={() => void act(item, 'start')}>
                    Start
                  </button>
                )}
                {item.status === 'IN_PROGRESS' && (
                  <button type="button" className="icon-button" onClick={() => void act(item, 'complete')}>
                    Complete
                  </button>
                )}
                {['COMPLETED', 'PENDING'].includes(item.status) && (
                  <button type="button" className="icon-button" onClick={() => void act(item, 'approve')}>
                    Approve
                  </button>
                )}
                {!['APPROVED', 'REJECTED'].includes(item.status) && (
                  <button type="button" className="icon-button" onClick={() => void act(item, 'reject')}>
                    Reject
                  </button>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}

      {/* SCCS Calculator Modal */}
      <Modal
        isOpen={calcModalOpen}
        onClose={() => setCalcModalOpen(false)}
        title="SCCS SED & Margin of Safety (MoS) Calculator"
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setCalcModalOpen(false)}>
              Close
            </button>
            <button
              type="button"
              className="btn btn-primary"
              disabled={!calcResult}
              onClick={applyCalculatedValues}
            >
              Apply to Evaluation
            </button>
          </>
        }
      >
        <p style={{ margin: '0 0 14px', fontSize: 13, color: 'var(--muted)' }}>
          Calculates Systemic Exposure Dose (SED) and Margin of Safety (MoS) according to the SCCS Notes of Guidance for testing cosmetic ingredients.
        </p>
        <form onSubmit={runSedMosCalculator} className="stack-form">
          <div className="calc-grid">
            <label>
              <span>Daily Amount A (g/day)</span>
              <input
                type="number"
                step="any"
                required
                value={calcForm.dailyAmountGrams}
                onChange={(e) => setCalcForm({ ...calcForm, dailyAmountGrams: e.target.value })}
              />
            </label>
            <label>
              <span>Concentration C (%)</span>
              <input
                type="number"
                step="any"
                required
                value={calcForm.concentrationPercent}
                onChange={(e) => setCalcForm({ ...calcForm, concentrationPercent: e.target.value })}
              />
            </label>
            <label>
              <span>Retention Factor R (0.01 - 1.0)</span>
              <input
                type="number"
                step="any"
                required
                value={calcForm.retentionFactor}
                onChange={(e) => setCalcForm({ ...calcForm, retentionFactor: e.target.value })}
              />
            </label>
            <label>
              <span>Dermal Absorption DAp (%)</span>
              <input
                type="number"
                step="any"
                required
                value={calcForm.dermalAbsorptionPercent}
                onChange={(e) => setCalcForm({ ...calcForm, dermalAbsorptionPercent: e.target.value })}
              />
            </label>
            <label>
              <span>NOAEL (mg/kg bw/day)</span>
              <input
                type="number"
                step="any"
                required
                value={calcForm.noaelMgKgDay}
                onChange={(e) => setCalcForm({ ...calcForm, noaelMgKgDay: e.target.value })}
              />
            </label>
            <label>
              <span>Body Weight BW (kg)</span>
              <input
                type="number"
                step="any"
                required
                value={calcForm.bodyWeightKg}
                onChange={(e) => setCalcForm({ ...calcForm, bodyWeightKg: e.target.value })}
              />
            </label>
          </div>
          <button type="submit" className="btn btn-secondary fluid" disabled={calcBusy}>
            {calcBusy ? 'Calculating...' : 'Recalculate SED & MoS'}
          </button>
        </form>

        {calcResult && (
          <div className={`calc-result-box ${calcResult.safe ? 'calc-result-safe' : 'calc-result-unsafe'}`}>
            <div className="calc-verdict">
              <span>{calcResult.safe ? '✅ SAFE' : '⚠️ UNSAFE'}</span>
              <span>— MoS: {calcResult.mosValue} (Threshold &ge; 100)</span>
            </div>
            <div style={{ fontSize: 13 }}>
              <strong>SED:</strong> {calcResult.sedValue} {calcResult.sedUnit}
            </div>
            <div style={{ fontSize: 12, opacity: 0.9 }}>
              {calcResult.formulaBreakdown}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}

const INFERENCE_TYPES = ['TOXICITY_ASSESSMENT', 'CPSR_GENERATION', 'SAFETY_REVIEW', 'REFERENCE_LOOKUP'];

export function LlmPanel({ requestId }) {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [prompt, setPrompt] = useState('');
  const [inferenceType, setInferenceType] = useState(INFERENCE_TYPES[0]);
  const [modelName, setModelName] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    try {
      setItems(await llmApi.list(requestId));
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load LLM requests.');
    } finally {
      setLoading(false);
    }
  }, [requestId]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!items.some((item) => ['PENDING', 'RUNNING'].includes(item.status))) return undefined;
    const timer = window.setInterval(() => void load(), 3000);
    return () => window.clearInterval(timer);
  }, [items, load]);

  async function submit(event) {
    event.preventDefault();
    if (!prompt.trim()) return;
    setSaving(true);
    setError(null);
    try {
      await llmApi.submit(requestId, {
        prompt,
        inferenceType,
        ...(modelName.trim() ? { modelName: modelName.trim() } : {}),
      });
      setPrompt('');
      toast.success('LLM analysis query submitted!');
      await load();
    } catch (caught) {
      const msg = caught instanceof ApiError ? caught.message : 'Unable to submit prompt.';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="panel subpanel">
      <div className="panel-header compact">
        <div>
          <p className="panel-eyebrow">AI safety assistant</p>
          <h3>LLM toxicity analysis</h3>
        </div>
      </div>
      <form className="mini-form" onSubmit={submit}>
        <textarea
          placeholder="Ask AI for regulatory analysis, toxicology screening, or hazard summary..."
          value={prompt}
          onChange={(event) => setPrompt(event.target.value)}
          required
        />
        <div className="grid-2">
          <select value={inferenceType} onChange={(event) => setInferenceType(event.target.value)}>
            {INFERENCE_TYPES.map((type) => (
              <option key={type} value={type}>
                {type.replaceAll('_', ' ')}
              </option>
            ))}
          </select>
          <input
            placeholder="Model name (e.g. gpt-4o-mini or leave blank)"
            value={modelName}
            onChange={(event) => setModelName(event.target.value)}
          />
        </div>
        <button className="btn btn-primary small" disabled={saving}>
          {saving ? 'Submitting...' : 'Run LLM Inference'}
        </button>
      </form>
      {error && <div className="error-box" role="alert">{error}</div>}
      {loading ? (
        <div className="empty-box small-pad">Loading LLM history...</div>
      ) : items.length === 0 ? (
        <div className="empty-box small-pad">No LLM analyses yet.</div>
      ) : (
        <ul className="tiny-list" style={{ marginTop: 14 }}>
          {items.map((item) => (
            <li key={item.id}>
              <span className="role-pill">{item.status}</span>
              {' '}{item.inferenceType?.replaceAll('_', ' ')}
              <small className="table-subtitle">{item.prompt}</small>
              {item.responseText && <div className="response-box">{item.responseText}</div>}
              {item.errorMessage && <div className="table-subtitle" style={{ color: '#f87171' }}>{item.errorMessage}</div>}
              {item.tokensUsed != null && (
                <small className="table-subtitle">
                  {item.tokensUsed} tokens {item.latencyMs != null ? `· ${item.latencyMs} ms` : ''} · model: {item.modelName || 'default'}
                </small>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
