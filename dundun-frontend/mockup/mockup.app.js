const { mainTabs, translations, createInitialState, architecture } = window.MockupBoot;
const Route = architecture.routes;
const state = createInitialState();

const domain = window.MockupDomain.create({ state, translations });
const {
    formatDate,
    formatKRW,
    getLang,
    tr,
    computeWageEstimate,
    getWorkproofCalendarContext,
    getDaysInMonth,
    getWorkproofMonthRecords,
    getWorkproofLevel,
    getWorkproofStatusKey,
    computeAdvanceSnapshot,
    computeVaultApplyAvailable,
    computeYieldSnapshot
} = domain;

const navigation = window.MockupNavigation.create({ state, mainTabs, Route });

function closeAllOverlays() {
    navigation.closeAllOverlays();
}

function openOverlay(id) {
    navigation.openOverlay(id);
}

function closeOverlay(id) {
    navigation.closeOverlay(id);
}

function getOpenLayers() {
    return navigation.getOpenLayers();
}

function setActiveNav(tabId) {
    navigation.setActiveNav(tabId);
}

function setHeaderForTab(tabId) {
    navigation.setHeaderForTab(tabId);
}

function setBottomNavForTab(tabId) {
    navigation.setBottomNavForTab(tabId);
}

function switchTab(tabId) {
    navigation.switchTab(tabId);
}

function showToast(message) {
    const toast = document.getElementById('toast');
    if (!toast) return;

    toast.textContent = message;
    toast.classList.remove('hidden');

    // Trigger transition
    window.requestAnimationFrame(() => {
        toast.classList.remove('opacity-0');
        toast.classList.add('opacity-100');
    });

    window.clearTimeout(showToast._timer);
    showToast._timer = window.setTimeout(() => {
        toast.classList.add('opacity-0');
        toast.classList.remove('opacity-100');
        window.setTimeout(() => toast.classList.add('hidden'), 200);
    }, 1200);
}

function getAuthState() {
    if (!state.ui.auth) {
        state.ui.auth = { loggedIn: false };
    }
    return state.ui.auth;
}

function getOnboardingState() {
    if (!state.ui.onboarding) {
        state.ui.onboarding = {
            completed: false,
            step: 1,
            mapVerified: false,
            wageUnit: 'hourly'
        };
    }
    return state.ui.onboarding;
}

function renderLanding() {
    const overlay = document.getElementById('overlay-landing');
    if (!overlay) return;
    overlay.classList.toggle('hidden', getAuthState().loggedIn);
}

function renderOnboardingHourlyPreview() {
    const previewEl = document.getElementById('onboarding-hourly-preview');
    if (!previewEl) return;

    const onboarding = getOnboardingState();
    const amountInput = document.getElementById('onboarding-wage-amount');
    const unitInput = document.getElementById('onboarding-wage-unit');
    const unitHoursInput = document.getElementById('onboarding-unit-hours');

    const amount = Number(amountInput?.value || 0);
    const unit = unitInput?.value || onboarding.wageUnit || 'hourly';
    const hours = Number(unitHoursInput?.value || 0);
    let hourly = 0;

    if (unit === 'hourly') {
        hourly = amount;
    } else if (unit === 'daily') {
        hourly = hours > 0 ? (amount / hours) : 0;
    } else {
        hourly = hours > 0 ? (amount / hours) : 0;
    }

    previewEl.textContent = `${formatKRW(Math.max(0, Math.floor(hourly)))} / h`;
}

function updateOnboardingUnitHint() {
    const onboarding = getOnboardingState();
    const unitInput = document.getElementById('onboarding-wage-unit');
    const wrap = document.getElementById('onboarding-unit-hours-wrap');
    const label = document.getElementById('onboarding-unit-hours-label');
    const hoursInput = document.getElementById('onboarding-unit-hours');
    if (!unitInput || !wrap || !label || !hoursInput) return;

    const unit = unitInput.value;
    onboarding.wageUnit = unit;

    if (unit === 'hourly') {
        wrap.classList.add('hidden');
        hoursInput.value = '';
    } else if (unit === 'daily') {
        wrap.classList.remove('hidden');
        label.textContent = '하루 기준 근무시간';
        if (!hoursInput.value) hoursInput.value = '8';
    } else {
        wrap.classList.remove('hidden');
        label.textContent = '월 기준 근무시간';
        if (!hoursInput.value) hoursInput.value = '209';
    }

    renderOnboardingHourlyPreview();
}

function renderOnboardingRecipientPreview() {
    const previewEl = document.getElementById('onboarding-recipient-preview');
    if (!previewEl) return;

    const rows = state.remittance.recipients;
    if (!rows.length) {
        previewEl.innerHTML = '<p class="text-[11px] text-slate-400 font-black">등록된 수신자가 없습니다.</p>';
        return;
    }

    previewEl.innerHTML = rows.map(recipient => {
        return `
            <div class="rounded-xl bg-white border border-slate-200 px-3 py-2">
                <p class="text-[11px] font-black text-slate-900">${recipient.name}</p>
                <p class="text-[10px] text-slate-500 font-mono mt-1 break-all">${recipient.address}</p>
            </div>
        `;
    }).join('');
}

function renderOnboarding() {
    const auth = getAuthState();
    const onboarding = getOnboardingState();
    const overlay = document.getElementById('overlay-onboarding');
    if (!overlay) return;

    const showOnboarding = auth.loggedIn && !onboarding.completed;
    overlay.classList.toggle('hidden', !showOnboarding);
    if (!showOnboarding) return;

    const step = onboarding.step || 1;
    const stepLabel = document.getElementById('onboarding-step-label');
    const progressBar = document.getElementById('onboarding-progress-bar');
    const stepWorkplace = document.getElementById('onboarding-step-workplace');
    const stepWage = document.getElementById('onboarding-step-wage');
    const stepRecipient = document.getElementById('onboarding-step-recipient');
    const prevBtn = document.getElementById('onboarding-prev-btn');
    const nextBtn = document.getElementById('onboarding-next-btn');
    const mapBtn = document.getElementById('onboarding-map-btn');
    const workplaceNameInput = document.getElementById('onboarding-workplace-name');
    const workplaceAddressInput = document.getElementById('onboarding-workplace-address');
    const wageUnitInput = document.getElementById('onboarding-wage-unit');
    const wageAmountInput = document.getElementById('onboarding-wage-amount');

    if (workplaceNameInput && !workplaceNameInput.value && state.workproof.workplaceName) {
        workplaceNameInput.value = state.workproof.workplaceName;
    }
    if (workplaceAddressInput && !workplaceAddressInput.value && state.workproof.workplaceAddress) {
        workplaceAddressInput.value = state.workproof.workplaceAddress;
    }
    if (wageUnitInput) wageUnitInput.value = onboarding.wageUnit || 'hourly';
    if (wageAmountInput && !wageAmountInput.value && state.wage.hourly) {
        wageAmountInput.value = String(state.wage.hourly);
    }

    if (stepLabel) stepLabel.textContent = `${step} / 3`;
    if (progressBar) progressBar.style.width = `${Math.floor((step / 3) * 100)}%`;
    if (stepWorkplace) stepWorkplace.classList.toggle('hidden', step !== 1);
    if (stepWage) stepWage.classList.toggle('hidden', step !== 2);
    if (stepRecipient) stepRecipient.classList.toggle('hidden', step !== 3);
    if (prevBtn) {
        prevBtn.disabled = step <= 1;
        prevBtn.classList.toggle('opacity-50', step <= 1);
    }
    if (nextBtn) {
        nextBtn.textContent = step === 3 ? '시작하기' : '다음';
    }
    if (mapBtn) {
        const cls = onboarding.mapVerified
            ? 'w-full py-3 rounded-2xl border border-emerald-200 bg-emerald-50 text-emerald-700 text-xs font-black'
            : 'w-full py-3 rounded-2xl border border-slate-200 bg-white text-slate-800 text-xs font-black';
        mapBtn.className = cls;
        mapBtn.innerHTML = onboarding.mapVerified
            ? '<i class="fa-solid fa-circle-check mr-2"></i>위치 확인 완료'
            : '<i class="fa-solid fa-map-location-dot mr-2 text-brand"></i>지도 위치 확인(데모)';
    }

    updateOnboardingUnitHint();
    renderOnboardingRecipientPreview();
}

function confirmWorkplaceMap() {
    const onboarding = getOnboardingState();
    onboarding.mapVerified = true;
    renderOnboarding();
}

function createMockWalletAddress() {
    const head = Math.random().toString(16).slice(2, 6).padEnd(4, '0');
    const tail = Math.random().toString(16).slice(2, 6).padEnd(4, '0');
    return `0x${head}...${tail}`;
}

function addOnboardingRecipient() {
    const nameInput = document.getElementById('onboarding-recipient-name');
    const name = (nameInput?.value || '').trim();

    if (!name) {
        showToast('수신자 이름을 입력해 주세요.');
        nameInput?.focus();
        return;
    }

    const seq = state.ui.recipientSeq || 1;
    const id = `R-${String(seq).padStart(3, '0')}`;
    state.ui.recipientSeq = seq + 1;
    state.remittance.recipients.push({
        id,
        name,
        address: createMockWalletAddress(),
        changedDay: state.demo.asOfDay
    });
    if (!state.remittance.selectedRecipientId) state.remittance.selectedRecipientId = id;
    if (nameInput) nameInput.value = '';

    renderOnboardingRecipientPreview();
    renderSend();
    showToast('수신자를 추가했습니다.');
}

function validateOnboardingStep(step) {
    if (step === 1) {
        const nameInput = document.getElementById('onboarding-workplace-name');
        const addressInput = document.getElementById('onboarding-workplace-address');
        const name = (nameInput?.value || '').trim();
        const address = (addressInput?.value || '').trim();
        const onboarding = getOnboardingState();

        if (!name) {
            showToast('근무지 이름을 입력해 주세요.');
            nameInput?.focus();
            return false;
        }
        if (!address) {
            showToast('근무지 주소를 입력해 주세요.');
            addressInput?.focus();
            return false;
        }
        if (!onboarding.mapVerified) {
            showToast('지도 위치 확인을 먼저 해주세요.');
            return false;
        }

        state.workproof.workplaceName = name;
        state.workproof.workplaceAddress = address;
        return true;
    }

    if (step === 2) {
        const amountInput = document.getElementById('onboarding-wage-amount');
        const unitInput = document.getElementById('onboarding-wage-unit');
        const unitHoursInput = document.getElementById('onboarding-unit-hours');
        const amount = Number(amountInput?.value || 0);
        const unit = unitInput?.value || 'hourly';
        const unitHours = Number(unitHoursInput?.value || 0);
        let hourly = 0;

        if (!Number.isFinite(amount) || amount <= 0) {
            showToast('기본급을 입력해 주세요.');
            amountInput?.focus();
            return false;
        }

        if (unit === 'hourly') {
            hourly = amount;
        } else {
            if (!Number.isFinite(unitHours) || unitHours <= 0) {
                showToast(unit === 'daily' ? '하루 기준 근무시간을 입력해 주세요.' : '월 기준 근무시간을 입력해 주세요.');
                unitHoursInput?.focus();
                return false;
            }
            hourly = amount / unitHours;
        }

        getOnboardingState().wageUnit = unit;
        state.wage.hourly = Math.max(1, Math.floor(hourly));
        return true;
    }

    if (!state.remittance.recipients.length) {
        showToast('수신자를 1명 이상 등록해 주세요.');
        return false;
    }
    if (!state.remittance.selectedRecipientId) {
        state.remittance.selectedRecipientId = state.remittance.recipients[0].id;
    }
    return true;
}

function completeOnboarding() {
    const onboarding = getOnboardingState();
    onboarding.completed = true;
    switchTab(Route.HOME);
    renderAll();
    showToast('초기 설정을 마쳤어요.');
}

function nextOnboardingStep() {
    const onboarding = getOnboardingState();
    const step = onboarding.step || 1;
    if (!validateOnboardingStep(step)) return;

    if (step >= 3) {
        completeOnboarding();
        return;
    }

    onboarding.step = step + 1;
    renderOnboarding();
}

function prevOnboardingStep() {
    const onboarding = getOnboardingState();
    onboarding.step = Math.max(1, (onboarding.step || 1) - 1);
    renderOnboarding();
}

function skipOnboardingDemo() {
    const onboarding = getOnboardingState();

    if (!state.workproof.workplaceName) state.workproof.workplaceName = 'Green Farm';
    if (!state.workproof.workplaceAddress) state.workproof.workplaceAddress = '경북 구미시 데모로 21';
    if (!state.wage.hourly) state.wage.hourly = 12000;
    if (!state.remittance.recipients.length) {
        state.remittance.recipients = [
            { id: 'R-001', name: 'Mama', address: '0x8f0a...12a9', changedDay: state.demo.asOfDay },
            { id: 'R-002', name: 'Anh', address: '0x1b6c...9c0d', changedDay: state.demo.asOfDay }
        ];
        state.ui.recipientSeq = 3;
    }
    if (!state.remittance.selectedRecipientId) {
        state.remittance.selectedRecipientId = state.remittance.recipients[0].id;
    }

    onboarding.completed = true;
    switchTab(Route.HOME);
    renderAll();
    showToast('데모 데이터로 바로 시작합니다.');
}

function mockLogin() {
    const auth = getAuthState();
    auth.loggedIn = true;
    renderAll();
    showToast('로그인되었습니다. 초기 설정을 시작해볼까요?');
}

function handleBackNavigation() {
    const auth = getAuthState();
    if (!auth.loggedIn) return;

    const onboarding = getOnboardingState();
    if (!onboarding.completed) {
        if ((onboarding.step || 1) > 1) {
            prevOnboardingStep();
        }
        return;
    }

    const openLayers = getOpenLayers();

    if (openLayers.length > 0) {
        openLayers[openLayers.length - 1].classList.add('hidden');
        return;
    }

    if (state.ui.currentTab === Route.TRANSFER) {
        if (state.remittance.flowStep === 'AMOUNT') {
            setTransferStep('RECIPIENT');
            return;
        }
        if (state.remittance.flowStep === 'RECIPIENT') {
            setTransferStep('ACCOUNT');
            return;
        }
        closeTransferPage();
        return;
    }

    if (state.ui.currentTab === Route.ACCOUNT_MANAGE) {
        closeManagePage();
        return;
    }

    if (window.history.length > 1) {
        window.history.back();
        return;
    }

    switchTab(Route.HOME);
}

function applyTranslations(lang) {
    const dict = translations[lang] || translations.ko;
    document.title = dict.doc_title;
    document.documentElement.setAttribute('lang', lang);
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (dict[key]) el.innerHTML = dict[key];
    });
}

function setLang(lang) {
    localStorage.setItem('wppLang', lang);
    applyTranslations(lang);
    document.querySelectorAll('[data-lang]').forEach(btn => {
        btn.classList.remove('bg-brand', 'text-white');
        btn.classList.add('bg-slate-100', 'text-slate-700');
    });
    const active = document.querySelector(`[data-lang="${lang}"]`);
    if (active) {
        active.classList.remove('bg-slate-100', 'text-slate-700');
        active.classList.add('bg-brand', 'text-white');
    }
    renderAll();
}

function anomalyThreshold(estimatedTotal) {
    const abs = state.wage.deductionsKnown ? 30000 : 50000;
    const pct = state.wage.deductionsKnown ? 0.02 : 0.03;
    return Math.max(abs, Math.floor(estimatedTotal * pct));
}

function getSelectedAccount() {
    return state.remittance.accounts.find(a => a.id === state.remittance.selectedAccountId) || state.remittance.accounts[0] || null;
}

function getSelectedRecipient() {
    return state.remittance.recipients.find(r => r.id === state.remittance.selectedRecipientId) || null;
}

function getFlowContext() {
    const est = computeWageEstimate();
    const actual = state.wage.actualDeposit || 0;
    const diff = Math.max(0, est.total - actual);
    const threshold = anomalyThreshold(est.total);
    const isAnomaly = diff >= threshold;
    const proof = getDocByType('PROOF_PACK');
    const claimKit = getDocByType('CLAIM_KIT');
    const selected = getSelectedRecipient();
    const remittanceBlocked = false;

    return { est, actual, diff, threshold, isAnomaly, proof, claimKit, selected, remittanceBlocked };
}

function resolveNextAction(ctx = getFlowContext()) {
    if (ctx.isAnomaly) {
        if (ctx.proof?.status !== 'READY') {
            return { id: 'make_proofpack', textKey: 'home_next_make_proofpack', textFallback: 'Would you like to organize evidence with Proof Pack first?', ctaKey: 'cta_make_proofpack', ctaFallback: 'Proof Pack', step: 1 };
        }
        if (ctx.claimKit?.status !== 'READY') {
            return { id: 'make_claimkit', textKey: 'home_next_make_claimkit', textFallback: 'Would you like to prepare a Claim Kit now?', ctaKey: 'cta_make_claimkit', ctaFallback: 'Open Claim Kit', step: 2 };
        }
        if (!state.claim.instantClaimOpened) {
            return { id: 'open_instant', textKey: 'home_next_open_instant', textFallback: 'Would you like to review the submission sentence in claim prep?', ctaKey: 'cta_instant_claim', ctaFallback: 'Open claim prep', step: 3 };
        }
    }

    if (state.remittance.status === 'CONFIRMED') {
        return { id: 'open_docs', textKey: 'home_next_done', textFallback: 'Flow complete. You can review receipts/documents anytime.', ctaKey: 'cta_open_docs', ctaFallback: 'Docs', step: 3 };
    }

    if (!ctx.isAnomaly) {
        return { id: 'open_send', textKey: 'home_next_no_anomaly', textFallback: 'No anomaly now; you can move to docs/remittance flow.', ctaKey: 'cta_open_send', ctaFallback: 'Review transfer', step: 3 };
    }

    return { id: 'open_send', textKey: 'home_next_send_ready', textFallback: 'You can transfer to an allow-list recipient when ready.', ctaKey: 'cta_open_send', ctaFallback: 'Review transfer', step: 3 };
}

function runFlowAction(actionId) {
    if (actionId === 'make_proofpack') {
        switchTab(Route.WAGE);
        requestDoc('proof');
        return;
    }
    if (actionId === 'make_claimkit') {
        switchTab(Route.WAGE);
        requestDoc('claim');
        return;
    }
    if (actionId === 'open_instant') {
        switchTab(Route.WAGE);
        openInstantClaim();
        return;
    }
    if (actionId === 'open_docs') {
        switchTab(Route.MENU);
        return;
    }
    switchTab(Route.FINANCE);
}

function runHomeNextAction() {
    const btn = document.getElementById('home-next-action-btn');
    const actionId = btn?.dataset.action || 'open_send';
    runFlowAction(actionId);
}

function runWagePrimaryAction() {
    const btn = document.getElementById('wage-primary-btn');
    const actionId = btn?.dataset.action || 'open_send';
    runFlowAction(actionId);
}

function setFlowStatus(elementId, tone, label) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.className = `flow-status ${tone}`;
    el.textContent = label;
}

function renderHome() {
    const ctx = getFlowContext();
    const selectedRecipient = ctx.selected;
    const selectedAccount = getSelectedAccount();
    const accountBalance = selectedAccount ? selectedAccount.balance : (ctx.actual || 0);
    const sendableAmount = Math.floor((accountBalance || 0) * 0.3);

    const accountBalanceEl = document.getElementById('home-account-balance');
    if (accountBalanceEl) accountBalanceEl.textContent = formatKRW(accountBalance);
    const sendableEl = document.getElementById('home-sendable');
    if (sendableEl) sendableEl.textContent = formatKRW(sendableAmount);
    const accountNameEl = document.getElementById('home-account-name');
    if (accountNameEl) accountNameEl.textContent = selectedAccount ? `${selectedAccount.name} · ${selectedAccount.number}` : '-';

    const todayDateEl = document.getElementById('today-date');
    if (todayDateEl) todayDateEl.textContent = formatDate(state.demo.asOfDay);
    const todayPlaceEl = document.getElementById('today-place');
    if (todayPlaceEl) todayPlaceEl.textContent = state.workproof.workplaceName;
    const todayStatusEl = document.getElementById('today-status');
    if (todayStatusEl) {
        const status = state.workproof.today.clockIn
            ? (state.workproof.today.clockOut ? tr('status_done', 'DONE') : tr('status_in_progress', 'IN PROGRESS'))
            : tr('status_ready', 'READY');
        todayStatusEl.textContent = status;
    }
    const homeClockInEl = document.getElementById('home-clock-in-time');
    if (homeClockInEl) homeClockInEl.textContent = state.workproof.today.clockIn || '-';
    const homeClockOutEl = document.getElementById('home-clock-out-time');
    if (homeClockOutEl) homeClockOutEl.textContent = state.workproof.today.clockOut || '-';

    const actualEl = document.getElementById('home-actual');
    if (actualEl) actualEl.textContent = formatKRW(ctx.actual);
    const briefEl = document.getElementById('home-money-brief');
    if (briefEl) {
        briefEl.textContent = `${tr('estimated', '추정')} ${formatKRW(ctx.est.total)} · ${tr('actual', '실제')} ${formatKRW(ctx.actual)}`;
    }

    const asofLabelEl = document.getElementById('asof-label');
    if (asofLabelEl) asofLabelEl.textContent = formatDate(state.demo.asOfDay);
    document.getElementById('wage-month').textContent = `${state.demo.year}-${String(state.demo.month).padStart(2, '0')}`;

    const isAnomaly = ctx.isAnomaly;
    const moneyStatusEl = document.getElementById('home-money-status');
    if (moneyStatusEl) {
        moneyStatusEl.className = `home-money-status ${isAnomaly ? 'alert' : 'ok'}`;
        moneyStatusEl.textContent = isAnomaly
            ? tr('diff_anomaly_title', 'Difference to review')
            : tr('diff_ok_title', 'No anomaly');
    }

    const moneyKickerEl = document.getElementById('home-money-kicker');
    if (moneyKickerEl) {
        moneyKickerEl.textContent = isAnomaly
            ? tr('diff_anomaly_kicker', 'REVIEW DIFF')
            : tr('diff_ok_kicker', 'NO ANOMALY');
    }

    const diffEl = document.getElementById('home-diff');
    if (diffEl) {
        diffEl.textContent = formatKRW(ctx.diff);
        diffEl.classList.toggle('text-rose-600', isAnomaly);
        diffEl.classList.toggle('text-emerald-600', !isAnomaly);
    }

    const next = resolveNextAction(ctx);
    const nextCard = document.getElementById('home-next-card');
    const urgentActions = ['make_proofpack'];
    const showNextAction = urgentActions.includes(next.id);
    if (nextCard) {
        nextCard.classList.toggle('hidden', !showNextAction);
    }

    const nextActionEl = document.getElementById('home-next-action');
    if (nextActionEl && showNextAction) {
        nextActionEl.textContent = tr(next.textKey, next.textFallback);
    }

    const nextBtn = document.getElementById('home-next-action-btn');
    if (nextBtn && showNextAction) {
        nextBtn.dataset.action = next.id;
        nextBtn.textContent = tr(next.ctaKey, next.ctaFallback);
        nextBtn.className = 'primary-btn action-cta w-full py-3 text-xs font-black';
    }
}

function renderWorkproofCalendar(records) {
    const weekEl = document.getElementById('workproof-calendar-week');
    const gridEl = document.getElementById('workproof-calendar-grid');
    const monthEl = document.getElementById('workproof-calendar-month');
    const countEl = document.getElementById('workproof-calendar-count');
    if (!weekEl || !gridEl || !monthEl || !countEl) return;

    const { year, month } = getWorkproofCalendarContext();
    const mm = String(month).padStart(2, '0');
    monthEl.textContent = `${year}.${mm}`;

    const weekLabels = getLang() === 'ko'
        ? ['일', '월', '화', '수', '목', '금', '토']
        : ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    weekEl.innerHTML = weekLabels.map(label => `<span>${label}</span>`).join('');

    const dayCount = getDaysInMonth(year, month);
    const firstWeekday = new Date(year, month - 1, 1).getDay();
    const recordMap = new Map(records.map(r => [r.day, r]));
    const uniqueDays = new Set(records.map(r => r.day)).size;
    countEl.textContent = `${tr('workproof_calendar_count', '기록일')} ${uniqueDays}${tr('days', '일')}`;

    let html = '';
    for (let i = 0; i < firstWeekday; i += 1) {
        html += '<span class="workproof-calendar-empty"></span>';
    }

    for (let day = 1; day <= dayCount; day += 1) {
        const record = recordMap.get(day) || null;
        const level = getWorkproofLevel(record);
        const statusText = tr(getWorkproofStatusKey(level), 'No record');
        const isCurrentDay = year === state.demo.year && month === state.demo.month && day === state.demo.asOfDay;
        html += `
            <button type="button" onclick="openWorkproofDay(${day})"
                class="workproof-calendar-cell level-${level}${isCurrentDay ? ' current' : ''}"
                title="${year}-${mm}-${String(day).padStart(2, '0')} · ${statusText}">
                <span>${day}</span>
            </button>
        `;
    }

    gridEl.innerHTML = html;
}

function moveWorkproofCalendar(delta) {
    state.workproof.calendarOffset = (state.workproof.calendarOffset || 0) + delta;
    renderWorkproof();
}

function openWorkproofDay(day) {
    const ctx = getWorkproofCalendarContext();
    state.workproof.selectedCalendarDay = { year: ctx.year, month: ctx.month, day };
    renderWorkproofDayOverlay();
    openOverlay('overlay-workproof-day');
}

function openWorkproofEditFromCalendar() {
    const btn = document.getElementById('workproof-day-edit-btn');
    const recordId = btn?.dataset.recordId || '';
    if (!recordId) return;
    closeOverlay('overlay-workproof-day');
    openWorkproofEdit(recordId);
}

function renderWorkproofDayOverlay() {
    const selected = state.workproof.selectedCalendarDay;
    if (!selected) return;

    const dateEl = document.getElementById('workproof-day-date');
    const statusEl = document.getElementById('workproof-day-status');
    const inEl = document.getElementById('workproof-day-in');
    const outEl = document.getElementById('workproof-day-out');
    const reasonEl = document.getElementById('workproof-day-reason');
    const editBtn = document.getElementById('workproof-day-edit-btn');

    const mm = String(selected.month).padStart(2, '0');
    const dd = String(selected.day).padStart(2, '0');
    if (dateEl) dateEl.textContent = `${selected.year}-${mm}-${dd}`;

    const records = getWorkproofMonthRecords(selected.year, selected.month);
    const record = records.find(r => r.day === selected.day) || null;
    const level = getWorkproofLevel(record);
    const statusText = tr(getWorkproofStatusKey(level), 'No record');

    if (statusEl) statusEl.textContent = statusText;
    if (inEl) inEl.textContent = (record?.inTime && record.inTime !== '-') ? record.inTime : '-';
    if (outEl) outEl.textContent = (record?.outTime && record.outTime !== '-') ? record.outTime : '-';

    if (reasonEl) {
        if (!record) {
            reasonEl.textContent = tr('workproof_day_no_record', 'No record for this date.');
        } else if (record.modified) {
            const reason = [record.reasonKey ? tr(record.reasonKey, '') : '', record.reasonMemo].filter(Boolean).join(' · ');
            reasonEl.textContent = reason || tr('workproof_day_no_reason', 'No edit reason');
        } else {
            reasonEl.textContent = tr('workproof_day_no_reason', 'No edit reason');
        }
    }

    if (editBtn) {
        const canEdit = !!record && !record.synthetic;
        editBtn.disabled = !canEdit;
        editBtn.classList.toggle('opacity-50', !canEdit);
        editBtn.dataset.recordId = canEdit ? record.id : '';
    }
}

function renderWorkproof() {
    const list = document.getElementById('workproof-list');
    if (!list) return;
    const records = getWorkproofMonthRecords(state.demo.year, state.demo.month);
    const visible = records.slice(0, 4);
    const modifiedCount = records.filter(r => r.modified).length;

    const calendarCtx = getWorkproofCalendarContext();
    const calendarRecords = getWorkproofMonthRecords(calendarCtx.year, calendarCtx.month);
    renderWorkproofCalendar(calendarRecords);
    renderWorkproofDayOverlay();

    const summaryDaysEl = document.getElementById('workproof-summary-days');
    if (summaryDaysEl) summaryDaysEl.textContent = `${records.length}${tr('days', '일')}`;
    const summaryEditsEl = document.getElementById('workproof-summary-edits');
    if (summaryEditsEl) summaryEditsEl.textContent = `${modifiedCount}${tr('items', '건')}`;

    list.innerHTML = visible.map(r => {
        const badgeModified = r.modified
            ? `<span class="badge badge-warn"><i class="fa-solid fa-pen-to-square"></i>${tr('badge_edited', 'Edited')}</span>`
            : `<span class="badge"><i class="fa-solid fa-circle-check"></i>${tr('badge_log', 'Log')}</span>`;
        const reason = [r.reasonKey ? tr(r.reasonKey, '') : '', r.reasonMemo].filter(Boolean).join(' · ');
        const canEdit = !r.synthetic;
        return `
        <div class="bg-slate-50 border border-slate-100 p-3 rounded-2xl flex items-start justify-between gap-3">
            <div class="flex-1">
                <div class="flex items-center gap-2">
                    <p class="text-xs font-black text-slate-800">${formatDate(r.day)}</p>
                    ${badgeModified}
                </div>
                <p class="text-[11px] text-slate-600 mt-2 font-black">${tr('label_in', 'IN')} ${r.inTime} · ${tr('label_out', 'OUT')} ${r.outTime}</p>
                ${r.modified && reason ? `<p class="text-[11px] text-slate-500 mt-1 leading-relaxed"><span class="font-black">${tr('label_reason', 'Reason:')}</span> ${reason}</p>` : ''}
            </div>
            <button ${canEdit ? `onclick="openWorkproofEdit('${r.id}')"` : 'disabled'} class="px-3 py-2 rounded-xl bg-white border border-slate-200 text-slate-800 text-[11px] font-black ${canEdit ? '' : 'opacity-50 cursor-not-allowed'}">${tr('cta_edit', 'Edit')}</button>
        </div>
    `;
    }).join('');

    const audit = state.workproof.audit[0];
    const auditPreview = document.getElementById('audit-preview');
    if (auditPreview && audit) {
        const auditReason = [audit.reasonKey ? tr(audit.reasonKey, '') : '', audit.reasonMemo].filter(Boolean).join(' · ');
        const reasonText = auditReason || '-';
        auditPreview.innerHTML = `
            <p class="audit-main">${audit.before} → ${audit.after}</p>
            <p class="audit-sub">${tr('label_reason', 'Reason:')} ${reasonText}</p>
            <p class="audit-sub">${tr('badge_attach', 'Attachments')} ${audit.attachments} · ${audit.at}</p>
        `;
    }

    setClockButtons();
}

function renderWage() {
    const ctx = getFlowContext();
    const est = ctx.est;
    document.getElementById('wage-base').textContent = formatKRW(est.base);
    document.getElementById('wage-ot-premium').textContent = formatKRW(est.otPremium);
    document.getElementById('wage-night-premium').textContent = formatKRW(est.nightPremium);
    document.getElementById('wage-estimated-total').textContent = formatKRW(est.total);

    const actual = ctx.actual;
    const diff = ctx.diff;

    document.getElementById('diff-estimated').textContent = formatKRW(est.total);
    document.getElementById('diff-actual').textContent = formatKRW(actual);
    document.getElementById('diff-amount').textContent = formatKRW(diff);

    const isAnomaly = ctx.isAnomaly;

    const kicker = document.getElementById('diff-kicker');
    const iconWrap = document.getElementById('diff-icon-wrap');
    const icon = document.getElementById('diff-icon');
    const title = document.getElementById('diff-title');
    const desc = document.getElementById('diff-desc');
    if (isAnomaly) {
        kicker.textContent = tr('diff_anomaly_kicker', 'REVIEW DIFF');
        kicker.className = 'text-[10px] font-black tracking-widest uppercase text-rose-600';
        iconWrap.className = 'w-12 h-12 rounded-2xl bg-rose-50 flex items-center justify-center text-rose-600';
        icon.className = 'fa-solid fa-triangle-exclamation text-xl';
        if (title) title.textContent = tr('diff_anomaly_title', 'Difference to review');
        if (desc) desc.textContent = tr('diff_anomaly_desc', 'There seems to be a difference between estimate and actual deposit. Want to review evidence first?');
    } else {
        kicker.textContent = tr('diff_ok_kicker', 'NO ANOMALY');
        kicker.className = 'text-[10px] font-black tracking-widest uppercase text-emerald-600';
        iconWrap.className = 'w-12 h-12 rounded-2xl bg-emerald-50 flex items-center justify-center text-emerald-600';
        icon.className = 'fa-solid fa-circle-check text-xl';
        if (title) title.textContent = tr('diff_ok_title', 'No anomaly');
        if (desc) desc.textContent = tr('diff_ok_desc', 'No clear anomaly based on current inputs.');
    }

    const input = document.getElementById('actual-deposit');
    if (input && Number(input.value) !== actual) input.value = String(actual);

    const modCount = state.workproof.records.filter(r => r.modified).length;
    const modEl = document.getElementById('wage-modcount');
    if (modEl) modEl.textContent = String(modCount);

    const proof = ctx.proof;
    const claimKit = ctx.claimKit;

    if (proof?.status === 'READY') {
        setFlowStatus('wage-step-proof-status', 'done', tr('flow_status_done', 'DONE'));
    } else if (proof?.status === 'QUEUED') {
        setFlowStatus('wage-step-proof-status', 'progress', tr('flow_status_progress', 'IN PROGRESS'));
    } else {
        setFlowStatus('wage-step-proof-status', 'ready', tr('flow_status_ready', 'READY'));
    }

    if (claimKit?.status === 'READY') {
        setFlowStatus('wage-step-claim-status', 'done', tr('flow_status_done', 'DONE'));
    } else if (claimKit?.status === 'QUEUED') {
        setFlowStatus('wage-step-claim-status', 'progress', tr('flow_status_progress', 'IN PROGRESS'));
    } else {
        setFlowStatus('wage-step-claim-status', 'ready', tr('flow_status_ready', 'READY'));
    }

    if (state.claim.instantClaimOpened) {
        setFlowStatus('wage-step-instant-status', 'done', tr('flow_status_done', 'DONE'));
    } else {
        setFlowStatus('wage-step-instant-status', 'ready', tr('flow_status_ready', 'READY'));
    }

    const next = resolveNextAction(ctx);
    const primaryDesc = document.getElementById('wage-primary-desc');
    if (primaryDesc) primaryDesc.textContent = tr(next.textKey, next.textFallback);

    const primaryStep = document.getElementById('wage-primary-step');
    if (primaryStep) primaryStep.textContent = `${tr('wage_step_word', 'STEP')} ${next.step}`;

    const primaryBtn = document.getElementById('wage-primary-btn');
    if (primaryBtn) {
        primaryBtn.dataset.action = next.id;
        primaryBtn.textContent = tr(next.ctaKey, next.ctaFallback);
        primaryBtn.className = 'primary-btn action-cta w-full py-4 text-xs font-black';
    }

    const secondaryWrap = document.getElementById('wage-secondary-actions');
    if (secondaryWrap) secondaryWrap.classList.toggle('hidden', !state.wage.showSecondaryActions);
    const secondaryToggle = document.getElementById('wage-secondary-toggle');
    if (secondaryToggle) {
        secondaryToggle.textContent = state.wage.showSecondaryActions
            ? tr('wage_hide_actions', 'Hide actions')
            : tr('wage_more_actions', 'More actions');
    }
}

function toggleWageSecondaryActions() {
    state.wage.showSecondaryActions = !state.wage.showSecondaryActions;
    renderWage();
}

function docDisplayTitle(doc) {
    const lang = getLang();
    const month = doc.month || `${state.demo.year}-${String(state.demo.month).padStart(2, '0')}`;
    const sep = '\u00A0·\u00A0';
    if (doc.type === 'PROOF_PACK') return lang === 'en' ? `Proof Pack${sep}${month}` : '증빙 리포트';
    if (doc.type === 'CLAIM_KIT') return lang === 'en' ? `Claim Kit${sep}${month}` : '근거 자료 묶음';
    if (doc.type === 'TRANSFER_RECEIPT') {
        const no = doc.receiptNo ? `#${doc.receiptNo}` : '';
        return lang === 'en' ? `Transfer Receipt${sep}${no}` : '송금 영수증';
    }
    return doc.type || 'Document';
}

function docMetaLine(doc) {
    const month = doc.month || `${state.demo.year}-${String(state.demo.month).padStart(2, '0')}`;
    const updated = doc.updatedAt ? doc.updatedAt : '—';
    if (doc.type === 'PROOF_PACK' || doc.type === 'CLAIM_KIT') return `${month} · ${updated}`;
    if (doc.type === 'TRANSFER_RECEIPT') {
        const no = doc.receiptNo ? `#${doc.receiptNo}` : '-';
        return `${no} · ${updated}`;
    }
    return updated;
}

function renderDocs() {
    const list = document.getElementById('docs-list');
    if (!list) return;

    const badgeFor = (status) => {
        if (status === 'READY') return `<span class="badge badge-ok"><i class="fa-solid fa-circle-check"></i>${tr('doc_status_ready', 'READY')}</span>`;
        if (status === 'QUEUED') return `<span class="badge"><i class="fa-solid fa-hourglass-half"></i>${tr('doc_status_queued', 'QUEUED')}</span>`;
        if (status === 'NOT_CREATED') return `<span class="badge"><i class="fa-regular fa-circle"></i>${tr('doc_status_not_yet', 'NOT YET')}</span>`;
        return `<span class="badge">${status}</span>`;
    };

    const iconFor = (type) => {
        if (type === 'PROOF_PACK') return 'fa-file-shield text-emerald-600 bg-emerald-50';
        if (type === 'CLAIM_KIT') return 'fa-box-archive text-slate-700 bg-slate-100';
        if (type === 'TRANSFER_RECEIPT') return 'fa-receipt text-brand bg-brand-soft';
        return 'fa-file-lines text-slate-600 bg-slate-100';
    };

    list.innerHTML = state.docs.map(doc => {
        const klass = iconFor(doc.type).split(' ');
        const icon = klass[0];
        const iconColor = klass[1];
        const iconBg = klass[2];
        const isReady = doc.status === 'READY';
        const passive = 'opacity-50 cursor-not-allowed';
        return `
        <div class="toss-card shadow-sm border border-white">
            <div class="flex items-start justify-between gap-4">
                <div class="w-12 h-12 rounded-2xl ${iconBg} flex items-center justify-center shrink-0 ${iconColor}">
                    <i class="fa-solid ${icon} text-xl"></i>
                </div>
                <div class="flex-1">
                    <div class="flex items-start justify-between gap-3">
                        <div>
                            <p class="doc-title">${docDisplayTitle(doc)}</p>
                            <p class="doc-meta">${docMetaLine(doc)}</p>
                        </div>
                        ${badgeFor(doc.status)}
                    </div>
                    <div class="grid grid-cols-2 gap-3 mt-4">
                        <button ${isReady ? '' : 'disabled'} class="py-3 rounded-2xl bg-slate-100 text-slate-800 text-xs font-black ${isReady ? '' : passive}"><i class="fa-solid fa-share-nodes mr-2"></i>${tr('cta_share', 'Share')}</button>
                        <button ${isReady ? '' : 'disabled'} class="py-3 rounded-2xl bg-brand-soft text-brand border border-indigo-100 text-xs font-black ${isReady ? '' : passive}"><i class="fa-solid fa-download mr-2"></i>${tr('cta_download', 'Download')}</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    }).join('');
}

function renderSend() {
    const selectedAccount = getSelectedAccount();

    const financeBalanceEl = document.getElementById('finance-account-balance');
    const financeAccountEl = document.getElementById('finance-send-account');
    const financeAvailableEl = document.getElementById('finance-send-available');
    const accountBalance = selectedAccount ? selectedAccount.balance : 0;
    const sendableAmount = Math.floor(accountBalance * 0.3);
    if (financeBalanceEl) financeBalanceEl.textContent = formatKRW(accountBalance);
    if (financeAccountEl) financeAccountEl.textContent = selectedAccount ? `${selectedAccount.name} · ${selectedAccount.number}` : '-';
    if (financeAvailableEl) {
        financeAvailableEl.textContent = formatKRW(sendableAmount);
    }

    const ctx = getFlowContext();
    const financeEstimated = document.getElementById('finance-estimated');
    const financeActual = document.getElementById('finance-actual');
    const financeDiff = document.getElementById('finance-diff');
    const financeDiffStatus = document.getElementById('finance-diff-status');
    if (financeEstimated) financeEstimated.textContent = formatKRW(ctx.est.total);
    if (financeActual) financeActual.textContent = formatKRW(ctx.actual);
    if (financeDiff) financeDiff.textContent = formatKRW(ctx.diff);
    if (financeDiff) {
        financeDiff.classList.toggle('text-rose-600', ctx.isAnomaly);
        financeDiff.classList.toggle('text-emerald-600', !ctx.isAnomaly);
    }
    if (financeDiffStatus) {
        financeDiffStatus.textContent = ctx.isAnomaly
            ? tr('diff_anomaly_desc', 'There seems to be a difference between estimate and actual deposit. Want to review evidence first?')
            : tr('diff_ok_desc', 'No clear anomaly based on current inputs.');
    }

    const advance = computeAdvanceSnapshot();
    const advanceBasis = getAdvanceCalendarSnapshot();
    const cardAdvanceAvailable = document.getElementById('finance-advance-available');
    const cardAdvanceRepayDate = document.getElementById('finance-advance-repay-date');
    const cardAdvanceProof = document.getElementById('finance-advance-proof');
    if (cardAdvanceAvailable) cardAdvanceAvailable.textContent = formatKRW(advance.available);
    if (cardAdvanceRepayDate) cardAdvanceRepayDate.textContent = advance.repayDate;
    if (cardAdvanceProof) {
        cardAdvanceProof.textContent = `${tr('advance_card_reflected', '근무 반영')} ${advanceBasis.reflected}${tr('days', '일')} · ${tr('advance_review_label', '확인 필요')} ${advanceBasis.review}${tr('days', '일')}`;
    }

    const yieldInfo = computeYieldSnapshot();
    const cardInterestBalance = document.getElementById('finance-interest-balance');
    const cardInterestApr = document.getElementById('finance-interest-apr');
    const cardInterestAccrued = document.getElementById('finance-interest-accrued');
    const cardInterestBreakdown = document.getElementById('finance-interest-breakdown');
    const cardInterestStatus = document.getElementById('finance-interest-status');
    const cardInterestOpenBtn = document.getElementById('finance-interest-open-btn');

    if (!state.vault.enabled) {
        if (cardInterestBalance) cardInterestBalance.textContent = tr('interest_not_enrolled_short', '미신청');
        if (cardInterestApr) cardInterestApr.textContent = `${(state.vault.apr * 100).toFixed(1)}%`;
        if (cardInterestAccrued) cardInterestAccrued.textContent = formatKRW(0);
        if (cardInterestStatus) cardInterestStatus.textContent = tr('interest_status_not_enrolled', '아직 예치를 시작하지 않았어요.');
        if (cardInterestBreakdown) cardInterestBreakdown.textContent = tr('interest_status_apply_hint', '예치 신청 후 이자 예상과 수익 구성을 확인할 수 있어요.');
        if (cardInterestOpenBtn) cardInterestOpenBtn.textContent = tr('finance_interest_open_apply', '신청');
    } else {
        if (cardInterestBalance) cardInterestBalance.textContent = formatKRW(yieldInfo.userDeposit);
        if (cardInterestApr) cardInterestApr.textContent = `${(state.vault.apr * 100).toFixed(1)}%`;
        if (cardInterestAccrued) cardInterestAccrued.textContent = formatKRW(state.vault.accruedInterest);
        if (cardInterestStatus) cardInterestStatus.textContent = tr('interest_status_enrolled', '예치가 활성화되어 이자가 적립 중이에요.');
        if (cardInterestBreakdown) {
            cardInterestBreakdown.textContent = `${tr('interest_breakdown_short', '월 수익')}: DeFi ${formatKRW(yieldInfo.defiMonthly)} + ${tr('interest_fee_short', '가불수수료')} ${formatKRW(yieldInfo.feeMonthly)}`;
        }
        if (cardInterestOpenBtn) cardInterestOpenBtn.textContent = tr('finance_interest_open', '상세');
    }

    renderAdvanceOverlay();
    renderInterestOverlay();
    renderTransferForm();
    renderTransferConfirm();
}

function getAdvanceCalendarSnapshot() {
    const year = state.demo.year;
    const month = state.demo.month;
    const asOfDay = state.demo.asOfDay;
    const dayCount = getDaysInMonth(year, month);
    const firstWeekday = new Date(year, month - 1, 1).getDay();
    const records = getWorkproofMonthRecords(year, month);
    const recordMap = new Map(records.map(r => [r.day, r]));

    let reflected = 0;
    let review = 0;
    let unreflected = 0;
    let html = '';

    for (let i = 0; i < firstWeekday; i += 1) {
        html += '<span class="advance-calendar-empty"></span>';
    }

    for (let day = 1; day <= dayCount; day += 1) {
        if (day > asOfDay) {
            html += `<span class="advance-calendar-cell future">${day}</span>`;
            continue;
        }

        const record = recordMap.get(day) || null;
        const level = getWorkproofLevel(record);
        let tone = 'unreflected';
        if (level === 2) {
            tone = 'reflected';
            reflected += 1;
        } else if (level === 3) {
            tone = 'review';
            review += 1;
        } else {
            unreflected += 1;
        }
        html += `<span class="advance-calendar-cell ${tone}">${day}</span>`;
    }

    const mm = String(month).padStart(2, '0');
    const dd = String(asOfDay).padStart(2, '0');
    return {
        reflected,
        review,
        unreflected,
        html,
        updatedAt: `${year}.${mm}.${dd}`
    };
}

function renderAdvanceOverlay() {
    const advance = computeAdvanceSnapshot();
    const basis = getAdvanceCalendarSnapshot();
    const availableEl = document.getElementById('advance-sheet-available');
    const usedEl = document.getElementById('advance-sheet-used');
    const receiveEl = document.getElementById('advance-sheet-receive');
    const feeEl = document.getElementById('advance-sheet-fee');
    const repayDateEl = document.getElementById('advance-sheet-repay-date');
    const requestBtn = document.getElementById('btn-advance-request');
    const daysEl = document.getElementById('advance-sheet-days');
    const hoursEl = document.getElementById('advance-sheet-hours');
    const verifiedWageEl = document.getElementById('advance-sheet-verified-wage');
    const chipsEl = document.getElementById('advance-amount-chips');
    const tierEl = document.getElementById('advance-sheet-tier');
    const historyEl = document.getElementById('advance-history-list');
    const reflectedEl = document.getElementById('advance-sheet-reflected-days');
    const reviewEl = document.getElementById('advance-sheet-review-days');
    const unreflectedEl = document.getElementById('advance-sheet-unreflected-days');
    const updatedAtEl = document.getElementById('advance-sheet-updated-at');
    const calendarGridEl = document.getElementById('advance-workproof-calendar-grid');

    if (availableEl) availableEl.textContent = formatKRW(advance.available);
    if (usedEl) usedEl.textContent = formatKRW(advance.used);
    if (receiveEl) receiveEl.textContent = formatKRW(advance.receive);
    if (feeEl) feeEl.textContent = formatKRW(advance.fee);
    if (repayDateEl) repayDateEl.textContent = advance.repayDate;
    if (daysEl) daysEl.textContent = `${advance.verifiedDays}${tr('days', '일')}`;
    if (hoursEl) hoursEl.textContent = `${advance.verifiedHours}${tr('hours', '시간')}`;
    if (verifiedWageEl) verifiedWageEl.textContent = formatKRW(advance.verifiedAmount);
    if (tierEl) tierEl.textContent = `${tr('advance_tier_label', '한도 단계')} ${advance.tierName}`;
    if (reflectedEl) reflectedEl.textContent = `${basis.reflected}${tr('days', '일')}`;
    if (reviewEl) reviewEl.textContent = `${basis.review}${tr('days', '일')}`;
    if (unreflectedEl) unreflectedEl.textContent = `${basis.unreflected}${tr('days', '일')}`;
    if (updatedAtEl) updatedAtEl.textContent = basis.updatedAt;
    if (calendarGridEl) calendarGridEl.innerHTML = basis.html;

    if (chipsEl) {
        chipsEl.innerHTML = advance.requestOptions.map(amount => {
            const enabled = amount <= advance.available;
            const selected = advance.requestAmount === amount;
            const cls = enabled
                ? (selected ? 'bg-brand text-white border-brand' : 'bg-white text-slate-700 border-slate-200')
                : 'bg-slate-100 text-slate-400 border-slate-200';
            return `<button ${enabled ? `onclick="selectAdvanceAmount(${amount})"` : 'disabled'} class="py-2 rounded-xl border text-[11px] font-black ${cls}">${Math.floor(amount / 10000)}만</button>`;
        }).join('');
    }

    if (historyEl) {
        const rows = state.advance.history.slice(0, 2);
        historyEl.innerHTML = rows.length
            ? rows.map(row => `
                <div class="flex items-center justify-between text-[11px]">
                    <span class="font-black text-slate-500">${row.date}</span>
                    <span class="font-black text-slate-900">${formatKRW(row.amount)} / ${row.status}</span>
                </div>
            `).join('')
            : `<p class="text-[11px] text-slate-400 font-black">${tr('advance_history_empty', '이력이 없습니다.')}</p>`;
    }

    if (requestBtn) {
        requestBtn.disabled = advance.requestAmount <= 0;
        requestBtn.classList.toggle('opacity-50', advance.requestAmount <= 0);
    }
}

function renderInterestOverlay() {
    const onboardSection = document.getElementById('interest-onboard-section');
    const activeSection = document.getElementById('interest-active-section');
    if (onboardSection) onboardSection.classList.toggle('hidden', state.vault.enabled);
    if (activeSection) activeSection.classList.toggle('hidden', !state.vault.enabled);

    const applyAvailable = computeVaultApplyAvailable();
    if (!state.vault.enabled && state.vault.selectedApplyAmount > applyAvailable) {
        const fallback = state.vault.applyOptions.filter(v => v <= applyAvailable).pop();
        state.vault.selectedApplyAmount = fallback || 100000;
    }
    const applyAmount = Math.min(state.vault.selectedApplyAmount, applyAvailable);
    const applyPreview = computeYieldSnapshot(applyAmount);

    const applyAvailableEl = document.getElementById('interest-apply-available');
    const applyAprEl = document.getElementById('interest-apply-apr');
    const applyMonthlyEl = document.getElementById('interest-apply-monthly');
    const applyDailyEl = document.getElementById('interest-apply-daily');
    const applyChipsEl = document.getElementById('interest-apply-chips');
    const applyBtn = document.getElementById('btn-interest-apply');

    if (applyAvailableEl) applyAvailableEl.textContent = formatKRW(applyAvailable);
    if (applyAprEl) applyAprEl.textContent = `${(state.vault.apr * 100).toFixed(1)}%`;
    if (applyMonthlyEl) applyMonthlyEl.textContent = formatKRW(applyPreview.monthlyTotal);
    if (applyDailyEl) applyDailyEl.textContent = formatKRW(applyPreview.dailyTotal);
    if (applyBtn) {
        applyBtn.disabled = applyAmount < 100000;
        applyBtn.classList.toggle('opacity-50', applyAmount < 100000);
    }

    if (applyChipsEl) {
        applyChipsEl.innerHTML = state.vault.applyOptions.map(amount => {
            const enabled = amount <= applyAvailable;
            const selected = state.vault.selectedApplyAmount === amount;
            const cls = enabled
                ? (selected ? 'bg-brand text-white border-brand' : 'bg-white text-slate-700 border-slate-200')
                : 'bg-slate-100 text-slate-400 border-slate-200';
            return `<button ${enabled ? `onclick="selectInterestApplyAmount(${amount})"` : 'disabled'} class="py-2 rounded-xl border text-[11px] font-black ${cls}">${Math.floor(amount / 10000)}만</button>`;
        }).join('');
    }

    const yieldInfo = computeYieldSnapshot();
    const balanceEl = document.getElementById('interest-balance');
    const aprEl = document.getElementById('interest-apr');
    const accruedEl = document.getElementById('interest-accrued');
    const monthlyEl = document.getElementById('interest-monthly');
    const dailyEl = document.getElementById('interest-daily');
    const defiMonthlyEl = document.getElementById('interest-defi-monthly');
    const feeMonthlyEl = document.getElementById('interest-fee-monthly');
    const totalMonthlyEl = document.getElementById('interest-total-monthly');
    const defiRatioEl = document.getElementById('interest-defi-ratio');
    const advanceRatioEl = document.getElementById('interest-advance-ratio');
    const advanceUsedEl = document.getElementById('interest-advance-used');
    const defiBar = document.getElementById('interest-defi-bar');
    const advanceBar = document.getElementById('interest-advance-bar');

    if (balanceEl) balanceEl.textContent = formatKRW(yieldInfo.userDeposit);
    if (aprEl) aprEl.textContent = `${(state.vault.apr * 100).toFixed(1)}%`;
    if (accruedEl) accruedEl.textContent = formatKRW(state.vault.accruedInterest);
    if (monthlyEl) monthlyEl.textContent = formatKRW(yieldInfo.monthlyTotal);
    if (dailyEl) dailyEl.textContent = formatKRW(yieldInfo.dailyTotal);
    if (defiMonthlyEl) defiMonthlyEl.textContent = formatKRW(yieldInfo.defiMonthly);
    if (feeMonthlyEl) feeMonthlyEl.textContent = formatKRW(yieldInfo.feeMonthly);
    if (totalMonthlyEl) totalMonthlyEl.textContent = formatKRW(yieldInfo.monthlyTotal);
    if (defiRatioEl) defiRatioEl.textContent = `${Math.round(yieldInfo.defiRatio * 100)}%`;
    if (advanceRatioEl) advanceRatioEl.textContent = `${Math.round(yieldInfo.advanceRatio * 100)}%`;
    if (advanceUsedEl) advanceUsedEl.textContent = `${Math.round(yieldInfo.advanceUtilization * 100)}% · ${formatKRW(yieldInfo.advanceUsedAmount)}`;
    if (defiBar) defiBar.style.width = `${Math.round(yieldInfo.defiRatio * 100)}%`;
    if (advanceBar) advanceBar.style.width = `${Math.round(yieldInfo.advanceRatio * 100)}%`;
}

function openAdvanceOverlay() {
    renderAdvanceOverlay();
    openOverlay('overlay-advance');
}

function openInterestOverlay() {
    renderInterestOverlay();
    openOverlay('overlay-interest');
}

function selectAdvanceAmount(amount) {
    state.advance.selectedRequest = amount;
    renderAdvanceOverlay();
}

function selectInterestApplyAmount(amount) {
    state.vault.selectedApplyAmount = amount;
    renderInterestOverlay();
}

function applyInterestVault() {
    const available = computeVaultApplyAvailable();
    const amount = Math.min(state.vault.selectedApplyAmount, available);
    if (amount < 100000) return;

    state.vault.enabled = true;
    state.vault.userDeposit = amount;
    state.vault.accruedInterest = Math.floor(computeYieldSnapshot(amount).dailyTotal * 6);

    renderSend();
    renderInterestOverlay();
    showToast(tr('toast_interest_applied', '예치 신청이 완료되었습니다.'));
}

function requestAdvance() {
    const advance = computeAdvanceSnapshot();
    if (advance.requestAmount <= 0) return;

    state.advance.used = Math.min(state.advance.maxCap, state.advance.used + advance.requestAmount);
    state.advance.history.unshift({
        id: `ADV-${String(Date.now()).slice(-6)}`,
        date: formatDate(state.demo.asOfDay),
        amount: advance.requestAmount,
        fee: advance.fee,
        status: '상환 예정'
    });
    if (Number(state.remittance.draftAmount || 0) <= 0) state.remittance.draftAmount = 50;

    renderSend();
    closeOverlay('overlay-advance');
    showToast(tr('toast_advance_done', 'Advance request submitted.'));
}

function renderAll() {
    renderHome();
    renderWorkproof();
    renderWage();
    renderSend();
    renderDocs();
    renderClaimOverlay();
    renderAccountManage();
    renderLanding();
    renderOnboarding();
}

function setClockButtons() {
    const inBtn = document.getElementById('btn-clockin');
    const outBtn = document.getElementById('btn-clockout');
    const inBtnHome = document.getElementById('btn-clockin-home');
    const outBtnHome = document.getElementById('btn-clockout-home');

    const hasIn = !!state.workproof.today.clockIn;
    const hasOut = !!state.workproof.today.clockOut;

    [inBtn, inBtnHome].forEach(btn => {
        if (!btn) return;
        btn.disabled = hasIn;
        btn.classList.toggle('opacity-50', hasIn);
    });
    [outBtn, outBtnHome].forEach(btn => {
        if (!btn) return;
        btn.disabled = !hasIn || hasOut;
        btn.classList.toggle('opacity-50', (!hasIn || hasOut));
    });
}

function clockIn() {
    if (state.workproof.today.clockIn) return;
    state.workproof.today.clockIn = '09:02';
    state.workproof.today.clockOut = null;
    const recordedTimeEl = document.getElementById('recorded-time');
    if (recordedTimeEl) recordedTimeEl.textContent = '09:02';
    renderAll();
}

function clockOut() {
    if (!state.workproof.today.clockIn || state.workproof.today.clockOut) return;
    state.workproof.today.clockOut = '18:04';
    const recordedTimeEl = document.getElementById('recorded-time');
    if (recordedTimeEl) recordedTimeEl.textContent = '18:04';
    renderAll();
}

function applyActualDeposit() {
    const input = document.getElementById('actual-deposit');
    if (!input) return;
    const v = Number(input.value);
    if (!Number.isFinite(v) || v <= 0) return;
    state.wage.actualDeposit = Math.floor(v);
    renderAll();
}

function requestDoc(kind) {
    const id = kind === 'proof' ? 'DOC-PROOF-2026-03' : 'DOC-CLAIM-2026-03';
    const existing = state.docs.find(d => d.id === id);
    if (existing) {
        existing.status = 'QUEUED';
        existing.updatedAt = `${formatDate(state.demo.asOfDay)} 15:02`;
    }
    renderDocs();
    renderClaimOverlay();
    renderWage();
    renderHome();

    window.setTimeout(() => {
        if (existing) {
            existing.status = 'READY';
            existing.updatedAt = `${formatDate(state.demo.asOfDay)} 15:03`;
        }
        renderDocs();
        renderClaimOverlay();
        renderWage();
        renderHome();
    }, 900);
}

function getDocByType(type) {
    return state.docs.find(d => d.type === type) || null;
}

function docStatusBadgeHTML(status) {
    if (status === 'READY') return `<span class="badge badge-ok"><i class="fa-solid fa-circle-check"></i>${tr('doc_status_ready', 'READY')}</span>`;
    if (status === 'QUEUED') return `<span class="badge"><i class="fa-solid fa-hourglass-half"></i>${tr('doc_status_queued', 'QUEUED')}</span>`;
    if (status === 'NOT_CREATED') return `<span class="badge"><i class="fa-regular fa-circle"></i>${tr('doc_status_not_yet', 'NOT YET')}</span>`;
    return `<span class="badge">${status || '-'}</span>`;
}

function setButtonEnabled(buttonEl, enabled) {
    if (!buttonEl) return;
    buttonEl.disabled = !enabled;
    buttonEl.classList.toggle('opacity-50', !enabled);
}

function renderClaimOverlay() {
    const proof = getDocByType('PROOF_PACK') || { status: 'NOT_CREATED' };
    const claim = getDocByType('CLAIM_KIT') || { status: 'NOT_CREATED' };

    const proofBadge = document.getElementById('claim-proofpack-badge');
    const claimBadge = document.getElementById('claim-claimkit-badge');
    if (proofBadge) proofBadge.innerHTML = docStatusBadgeHTML(proof.status);
    if (claimBadge) claimBadge.innerHTML = docStatusBadgeHTML(claim.status);

    const proofAction = document.getElementById('claim-proofpack-action');
    const claimAction = document.getElementById('claim-claimkit-action');

    if (proofAction) {
        if (proof.status === 'READY') proofAction.textContent = tr('cta_open', 'Open');
        else if (proof.status === 'QUEUED') proofAction.textContent = tr('cta_generating', 'Generating...');
        else proofAction.textContent = tr('cta_generate', 'Generate');
        setButtonEnabled(proofAction, proof.status !== 'QUEUED');
    }

    if (claimAction) {
        if (claim.status === 'READY') claimAction.textContent = tr('cta_open', 'Open');
        else if (claim.status === 'QUEUED') claimAction.textContent = tr('cta_generating', 'Generating...');
        else claimAction.textContent = tr('cta_generate', 'Generate');
        setButtonEnabled(claimAction, claim.status !== 'QUEUED');
    }

    const proofOpen = document.getElementById('claim-proofpack-open');
    const proofShare = document.getElementById('claim-proofpack-share');
    const claimOpen = document.getElementById('claim-claimkit-open');
    const claimShare = document.getElementById('claim-claimkit-share');
    setButtonEnabled(proofOpen, proof.status === 'READY');
    setButtonEnabled(proofShare, proof.status === 'READY');
    setButtonEnabled(claimOpen, claim.status === 'READY');
    setButtonEnabled(claimShare, claim.status === 'READY');

    const proofCheckIcon = document.getElementById('claim-check-proof-icon');
    const claimCheckIcon = document.getElementById('claim-check-claim-icon');
    if (proofCheckIcon) {
        proofCheckIcon.className = proof.status === 'READY'
            ? 'fa-solid fa-circle-check text-emerald-500'
            : (proof.status === 'QUEUED' ? 'fa-solid fa-circle-notch text-brand animate-spin' : 'fa-solid fa-circle-info text-brand');
    }
    if (claimCheckIcon) {
        claimCheckIcon.className = claim.status === 'READY'
            ? 'fa-solid fa-circle-check text-emerald-500'
            : (claim.status === 'QUEUED' ? 'fa-solid fa-circle-notch text-brand animate-spin' : 'fa-solid fa-circle-info text-brand');
    }

    const primary = document.getElementById('btn-claimkit-primary');
    if (primary) {
        const claimKitLabel = tr('doc_claimkit', 'Claim Kit');
        const generateLabel = getLang() === 'en'
            ? `${tr('cta_generate', 'Generate')} ${claimKitLabel}`
            : `${claimKitLabel} ${tr('cta_generate', 'Generate')}`;

        if (claim.status === 'READY') primary.textContent = tr('open_claimkit', 'Open Claim Kit');
        else if (claim.status === 'QUEUED') primary.textContent = tr('cta_generating', 'Generating...');
        else primary.textContent = generateLabel;

        setButtonEnabled(primary, claim.status !== 'QUEUED');
    }
}

function openOrCreateDocFromClaim(type) {
    const doc = getDocByType(type);
    if (!doc) return;
    if (doc.status === 'READY') return openDocFromClaim(type);
    if (doc.status === 'QUEUED') return showToast(tr('toast_generating_doc', 'Generating document...'));
    requestDoc(type === 'PROOF_PACK' ? 'proof' : 'claim');
    showToast(tr('toast_generating_doc', 'Generating document...'));
}

function openDocFromClaim(type) {
    const doc = getDocByType(type);
    if (!doc) return;
    if (doc.status !== 'READY') requestDoc(type === 'PROOF_PACK' ? 'proof' : 'claim');
    switchTab(Route.MENU);
    closeOverlay('overlay-claim');
}

function docShareLink(type) {
    const doc = getDocByType(type);
    const stamp = encodeURIComponent(String(Date.now()).slice(-6));
    if (type === 'PROOF_PACK') return `https://s3.example.com/dawndone/proof-pack-${doc?.month || '2026-03'}.pdf?demo=1&sig=${stamp}`;
    if (type === 'CLAIM_KIT') return `https://s3.example.com/dawndone/claim-kit-${doc?.month || '2026-03'}.pdf?demo=1&sig=${stamp}`;
    return `https://s3.example.com/dawndone/document.pdf?demo=1&sig=${stamp}`;
}

async function copyText(text, toastMessage) {
    try {
        await navigator.clipboard.writeText(text);
        showToast(toastMessage || tr('toast_copied', 'Copied'));
    } catch (e) {
        const tmp = document.createElement('textarea');
        tmp.value = text;
        document.body.appendChild(tmp);
        tmp.select();
        document.execCommand('copy');
        document.body.removeChild(tmp);
        showToast(toastMessage || tr('toast_copied', 'Copied'));
    }
}

function shareDocFromClaim(type) {
    const doc = getDocByType(type);
    if (!doc) return;
    if (doc.status !== 'READY') {
        requestDoc(type === 'PROOF_PACK' ? 'proof' : 'claim');
        showToast(tr('toast_generating_doc', 'Generating document...'));
        return;
    }
    copyText(docShareLink(type), tr('toast_link_copied', 'Link copied'));
}

function generateClaimSummary(style = 'default', showFeedback = false) {
    state.claim.summaryStyle = style;
    const est = computeWageEstimate();
    const diff = Math.max(0, est.total - (state.wage.actualDeposit || 0));
    const lang = getLang();

    let text = '';
    if (lang === 'en') {
        if (style === 'short') {
            text = `I have WorkProof evidence for 2026-03 and there is a difference of ${formatKRW(diff)} between my estimate (${formatKRW(est.total)}) and actual deposit (${formatKRW(state.wage.actualDeposit)}).`;
        } else if (style === 'firm') {
            text = `I am submitting evidence (WorkProof logs and change history) for my work in 2026-03. The reference estimate is ${formatKRW(est.total)}, the actual deposit was ${formatKRW(state.wage.actualDeposit)}, and the difference is ${formatKRW(diff)}. Please review and correct any missing payment.`;
        } else {
            text = `I have WorkProof records for my work in 2026-03. The reference estimate is ${formatKRW(est.total)}, but my actual deposit was ${formatKRW(state.wage.actualDeposit)}, resulting in a difference of ${formatKRW(diff)}. I organized relevant evidence (work logs, edit change history, attachments) into a Proof Pack and a Claim Kit.`;
        }
    } else {
        if (style === 'short') {
            text = `본인은 2026-03월 근무에 대해 근거 자료(WorkProof)를 보유하고 있으며, 추정 ${formatKRW(est.total)} 대비 실입금 ${formatKRW(state.wage.actualDeposit)}으로 차액 ${formatKRW(diff)}이 발생했습니다.`;
        } else if (style === 'firm') {
            text = `본인은 2026-03월 근무에 대한 근거 자료(WorkProof: 근무 로그/변경 기록/첨부)를 보유하고 있습니다. 참고용 추정 급여는 ${formatKRW(est.total)}이나 실제 입금액은 ${formatKRW(state.wage.actualDeposit)}으로 차액 ${formatKRW(diff)}이 발생했습니다. 관련 자료를 Proof Pack 및 근거 자료 묶음으로 정리하여 제출합니다.`;
        } else {
            text = `본인은 2026-03월 근무에 대해 근거 자료(WorkProof)를 보유하고 있으며, 참고용 추정 급여는 ${formatKRW(est.total)}이나 실제 입금액은 ${formatKRW(state.wage.actualDeposit)}으로 차액 ${formatKRW(diff)}이 발생했습니다. 관련 근거(근무 로그/변경 기록/첨부)를 Proof Pack 및 근거 자료 묶음으로 정리했습니다.`;
        }
    }

    const el = document.getElementById('claim-summary');
    if (el) el.textContent = text;
    if (showFeedback) showToast(tr('toast_generated', 'Generated'));
}

function openCopilot(mode) {
    const titleEl = document.getElementById('copilot-title');
    const answerEl = document.getElementById('copilot-answer');
    const est = computeWageEstimate();
    const diff = Math.max(0, est.total - (state.wage.actualDeposit || 0));

    const lang = getLang();
    const answers = lang === 'en' ? {
        wage_explain: {
            title: 'Wage Shield',
            text: `This screen compares a reference wage estimate (work hours, overtime/night premiums) with your actual deposit. If the difference exceeds the threshold, it shows evidence (WorkProof IDs and edit change history) and links you to Proof Pack, Claim Kit, and Instant Claim.`
        },
        wage_why_diff: {
            title: 'Why is there a difference?',
            text: `The estimated total is ${formatKRW(est.total)} and the actual deposit is ${formatKRW(state.wage.actualDeposit)}. The difference is ${formatKRW(diff)}. Overtime/night hours and edited logs are included in the estimate, so those items may not have been reflected in payment.`
        },
        wage_evidence: {
            title: 'What evidence is used?',
            text: `Evidence includes WorkProof logs (clock-in/out times + location snapshots) and edit change history (reason, before/after, attachment presence). Proof Pack/Claim Kit automatically include these as tables and logs.`
        },
        wage_next: {
            title: 'Next steps',
            text: `1) Generate Proof Pack → 2) Generate Claim Kit → 3) Use Instant Claim to prepare a submission sentence and checklist.`
        },
        claim_path_online: {
            title: 'Online submission (demo)',
            text: `Use the submission sentence you copied and attach/share your Claim Kit PDF. Keep your key facts handy: period (2026-03), estimate ${formatKRW(est.total)}, actual ${formatKRW(state.wage.actualDeposit)}, diff ${formatKRW(diff)}, and WorkProof evidence. (Demo: no real external link)`
        },
        claim_path_phone: {
            title: 'Phone consultation (demo)',
            text: `Before calling, prepare: the summary sentence, Claim Kit / Proof Pack files, your work period, and the difference amount (${formatKRW(diff)}). You can share the Claim Kit link from this screen. (Demo: no real dialing)`
        },
        claim_path_visit: {
            title: 'In-person visit (demo)',
            text: `Bring: your summary sentence, Claim Kit / Proof Pack, and any supporting evidence you have. Make sure edited WorkProof logs include reason/attachments in change history. (Demo guidance)`
        }
    } : {
        wage_explain: {
            title: 'Wage Shield',
            text: `이 화면은 참고용 추정 급여(근무시간, 연장/야간)와 실제 입금액을 비교해 확인이 필요한 차이를 보여줍니다. 차이가 임계값을 넘으면 근거 자료 ID와 수정 이력을 함께 보여주고, Proof Pack/근거 자료 묶음/신고 준비로 이어서 확인할 수 있어요.`
        },
        wage_why_diff: {
            title: '왜 차액이 생겼나요?',
            text: `현재 추정 합계는 ${formatKRW(est.total)}이고 실제 입금액은 ${formatKRW(state.wage.actualDeposit)}입니다. 차액은 ${formatKRW(diff)}입니다. 연장/야간 시간과 수정된 기록이 포함되어 있어, 해당 근거가 실제 지급에 반영되지 않았을 가능성이 있습니다.`
        },
        wage_evidence: {
            title: '근거는 무엇인가요?',
            text: `근거는 근거 자료(WorkProof: 출근/퇴근 시각 + 위치 스냅샷)와 수정 이력(사유, 변경 전/후, 첨부 유무)입니다. Proof Pack/근거 자료 묶음에서 이 근거를 표와 로그로 함께 확인할 수 있어요.`
        },
        wage_next: {
            title: '지금 할 수 있는 다음 행동',
            text: `1) Proof Pack 만들기 → 2) 근거 자료 묶음 생성 → 3) Instant Claim에서 제출용 문장/체크리스트를 확인해볼까요?`
        },
        claim_path_online: {
            title: '온라인 접수 안내(데모)',
            text: `복사한 제출용 문장을 붙여넣고 근거 자료 묶음 PDF를 첨부/공유해 주세요. 핵심 사실: 기간(2026-03), 추정 ${formatKRW(est.total)}, 실입금 ${formatKRW(state.wage.actualDeposit)}, 차액 ${formatKRW(diff)}, 근거 자료(WorkProof). (데모: 실제 외부 링크 없음)`
        },
        claim_path_phone: {
            title: '전화 접수 안내(데모)',
            text: `전화 상담/접수 전에 제출용 문장, 근거 자료 묶음/Proof Pack 파일, 근무 기간, 차액 금액(${formatKRW(diff)})을 준비해 두면 좋아요. 이 화면에서 근거 자료 묶음 링크를 복사해 전달할 수도 있어요.`
        },
        claim_path_visit: {
            title: '방문 접수 안내(데모)',
            text: `방문 시에는 제출용 문장과 근거 자료 묶음/Proof Pack을 함께 준비해 주세요. 근거 자료(WorkProof) 수정 기록은 사유/첨부가 변경 기록에 남아 증빙에 포함됩니다.`
        }
    };

    const payload = answers[mode] || answers.wage_explain;
    if (titleEl) titleEl.textContent = payload.title;
    if (answerEl) answerEl.textContent = payload.text;
    openOverlay('overlay-copilot');
}

function openInstantClaim() {
    state.claim.instantClaimOpened = true;
    generateClaimSummary('default', false);
    renderClaimOverlay();
    renderWage();
    renderHome();
    openOverlay('overlay-claim');
}

function openWorkproofEdit(recordId) {
    state.editing.recordId = recordId;
    const record = state.workproof.records.find(r => r.id === recordId);
    const label = document.getElementById('edit-record-id');
    if (label) {
        label.textContent = record
            ? `${formatDate(record.day)} · ${record.inTime}–${record.outTime}`
            : formatDate(state.demo.asOfDay);
    }
    const reason = document.getElementById('edit-reason');
    if (reason) reason.value = '';
    const memo = document.getElementById('edit-memo');
    if (memo) memo.value = '';
    openOverlay('overlay-workproof-edit');
}

function saveWorkproofEdit() {
    const recordId = state.editing.recordId;
    const record = state.workproof.records.find(r => r.id === recordId);
    if (!record) return;

    const reasonSel = document.getElementById('edit-reason');
    const memo = document.getElementById('edit-memo');
    if (!reasonSel || !reasonSel.value) {
        reasonSel?.focus();
        return;
    }

    const reasonKeyByValue = {
        late_tap: 'reason_1',
        overtime: 'reason_2',
        break: 'reason_3',
        missing: 'reason_4',
        other: 'reason_5'
    };

    record.modified = true;
    record.reasonKey = reasonKeyByValue[reasonSel.value] || 'reason_5';
    record.reasonMemo = memo && memo.value ? memo.value.trim() : null;
    record.attachments = Math.max(record.attachments, 1);

    state.workproof.audit.unshift({
        id: record.id,
        before: '09:01–18:03',
        after: `${record.inTime}–${record.outTime}`,
        reasonKey: record.reasonKey,
        reasonMemo: record.reasonMemo,
        attachments: record.attachments,
        at: `${formatDate(state.demo.asOfDay)} 12:34`
    });

    renderWorkproof();
    closeOverlay('overlay-workproof-edit');
}

async function copyFrom(elementId, isTextNode = false) {
    let text = '';
    const el = document.getElementById(elementId);
    if (!el) return;

    if (isTextNode) text = el.textContent || '';
    else text = el.value || el.textContent || '';

    try {
        await navigator.clipboard.writeText(text);
        showToast(tr('toast_copied', 'Copied'));
    } catch (e) {
        const tmp = document.createElement('textarea');
        tmp.value = text;
        document.body.appendChild(tmp);
        tmp.select();
        document.execCommand('copy');
        document.body.removeChild(tmp);
        showToast(tr('toast_copied', 'Copied'));
    }
}

function openAccountManage() {
    if (state.ui.currentTab !== Route.ACCOUNT_MANAGE) {
        state.ui.manageReturnTab = state.ui.currentTab || Route.FINANCE;
    }
    renderAccountManage();
    switchTab(Route.ACCOUNT_MANAGE);
}

function closeManagePage() {
    switchTab(state.ui.manageReturnTab || Route.FINANCE);
}

function renderAccountManage() {
    const accountListEl = document.getElementById('manage-account-list');
    const recipientListEl = document.getElementById('manage-recipient-list');

    if (accountListEl) {
        accountListEl.innerHTML = state.remittance.accounts.map(account => {
            const active = account.id === state.remittance.selectedAccountId;
            const cls = active ? 'border-brand bg-brand-soft' : 'border-slate-200 bg-white';
            return `
                <button type="button" onclick="selectTransferAccount('${account.id}')"
                    class="w-full rounded-2xl border px-4 py-3 text-left ${cls}">
                    <p class="text-xs font-black text-slate-900">${account.name}</p>
                    <p class="text-[11px] font-black mt-1 text-slate-600">${account.number} · ${formatKRW(account.balance)}</p>
                </button>
            `;
        }).join('');
    }

    if (recipientListEl) {
        recipientListEl.innerHTML = state.remittance.recipients.map(recipient => {
            const active = recipient.id === state.remittance.selectedRecipientId;
            const cls = active ? 'border-brand bg-brand-soft' : 'border-slate-200 bg-white';
            return `
                <button type="button" onclick="selectTransferRecipient('${recipient.id}')"
                    class="w-full rounded-2xl border px-4 py-3 text-left ${cls}">
                    <p class="text-xs font-black text-slate-900">${recipient.name}</p>
                    <p class="text-[11px] font-mono text-slate-500 mt-1 break-all">${recipient.address}</p>
                </button>
            `;
        }).join('');
    }
}

function addMockAccount() {
    const seq = state.ui.accountSeq || (state.remittance.accounts.length + 1);
    const id = `A-${String(seq).padStart(3, '0')}`;
    const baseName = getLang() === 'en' ? 'Extra Account' : '추가 계좌';
    state.ui.accountSeq = seq + 1;
    state.remittance.accounts.push({
        id,
        name: `${baseName} ${seq - 1}`,
        number: `****-${String(1000 + seq).slice(-4)}`,
        balance: 0
    });
    renderAll();
    showToast(tr('toast_account_added', 'Account added.'));
}

function addMockRecipient() {
    const seq = state.ui.recipientSeq || (state.remittance.recipients.length + 1);
    const id = `R-${String(seq).padStart(3, '0')}`;
    const baseName = getLang() === 'en' ? 'New Wallet' : '새 지갑';
    state.ui.recipientSeq = seq + 1;
    state.remittance.recipients.push({
        id,
        name: `${baseName} ${seq - 1}`,
        address: createMockWalletAddress(),
        changedDay: state.demo.asOfDay
    });
    renderAll();
    showToast(tr('toast_wallet_added', 'Recipient wallet added.'));
}

function openTransferSheet() {
    if (!getSelectedAccount() && state.remittance.accounts[0]) {
        state.remittance.selectedAccountId = state.remittance.accounts[0].id;
    }
    if (!getSelectedRecipient() && state.remittance.recipients[0]) {
        state.remittance.selectedRecipientId = state.remittance.recipients[0].id;
    }
    if (state.ui.currentTab !== Route.TRANSFER) {
        state.ui.transferReturnTab = state.ui.currentTab || Route.FINANCE;
    }
    state.remittance.flowStep = 'ACCOUNT';
    switchTab(Route.TRANSFER);
    renderTransferForm();
}

function setTransferStep(step) {
    if (step === 'ACCOUNT') state.remittance.flowStep = 'ACCOUNT';
    else if (step === 'AMOUNT') state.remittance.flowStep = 'AMOUNT';
    else state.remittance.flowStep = 'RECIPIENT';
    renderTransferForm();
}

function closeTransferPage() {
    switchTab(state.ui.transferReturnTab || Route.FINANCE);
}

function goBackTransferStep() {
    if (state.remittance.flowStep === 'AMOUNT') {
        setTransferStep('RECIPIENT');
        return;
    }
    if (state.remittance.flowStep === 'RECIPIENT') {
        setTransferStep('ACCOUNT');
        return;
    }
    closeTransferPage();
}

function selectTransferAccount(accountId) {
    state.remittance.selectedAccountId = accountId;
    const fromManage = state.ui.currentTab === Route.ACCOUNT_MANAGE;
    const returnTab = fromManage ? (state.ui.manageReturnTab || Route.FINANCE) : state.ui.currentTab;
    const inTransferFlow = returnTab === Route.TRANSFER;
    if (inTransferFlow) {
        state.remittance.flowStep = 'RECIPIENT';
        switchTab(Route.TRANSFER);
    }
    if (inTransferFlow) {
        renderTransferForm();
    } else if (fromManage) {
        renderAccountManage();
    }
    renderHome();
    renderSend();
}

function selectTransferRecipient(recipientId) {
    state.remittance.selectedRecipientId = recipientId;
    const fromManage = state.ui.currentTab === Route.ACCOUNT_MANAGE;
    const returnTab = fromManage ? (state.ui.manageReturnTab || Route.FINANCE) : state.ui.currentTab;
    const inTransferFlow = returnTab === Route.TRANSFER;
    if (inTransferFlow) {
        state.remittance.flowStep = 'AMOUNT';
        switchTab(Route.TRANSFER);
    }
    if (inTransferFlow) {
        renderTransferForm();
    } else if (fromManage) {
        renderAccountManage();
    }
    renderSend();
}

function updateTransferAmount(value) {
    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed < 0) {
        state.remittance.draftAmount = 0;
    } else {
        state.remittance.draftAmount = Math.floor(parsed);
    }
    renderTransferForm();
    renderTransferConfirm();
}

function renderTransferForm() {
    const step = state.remittance.flowStep || 'ACCOUNT';
    const accountListEl = document.getElementById('transfer-account-list');
    const recipientListEl = document.getElementById('transfer-recipient-list');
    const amountInputEl = document.getElementById('transfer-amount-input');
    const amountHintEl = document.getElementById('transfer-amount-hint');
    const formTitleEl = document.getElementById('transfer-form-title');
    const formSubEl = document.getElementById('transfer-form-subtitle');
    const stepRecipientEl = document.getElementById('transfer-step-recipient');
    const stepAmountEl = document.getElementById('transfer-step-amount');
    const stepAccountEl = document.getElementById('transfer-step-account');
    const selectedRecipientNameEl = document.getElementById('transfer-selected-recipient-name');
    const selectedRecipientAddressEl = document.getElementById('transfer-selected-recipient-address');
    const selectedAccountNameEl = document.getElementById('transfer-selected-account-name');

    const selectedAccount = getSelectedAccount();
    const selectedRecipient = getSelectedRecipient();

    if (stepRecipientEl) stepRecipientEl.classList.toggle('hidden', step !== 'RECIPIENT');
    if (stepAmountEl) stepAmountEl.classList.toggle('hidden', step !== 'AMOUNT');
    if (stepAccountEl) stepAccountEl.classList.toggle('hidden', step !== 'ACCOUNT');
    if (formTitleEl) {
        if (step === 'AMOUNT') formTitleEl.textContent = tr('transfer_step_amount_title', 'Enter amount');
        else if (step === 'ACCOUNT') formTitleEl.textContent = tr('transfer_step_account_title', 'Choose account');
        else formTitleEl.textContent = tr('transfer_step_recipient_title', 'Choose recipient');
    }
    if (formSubEl) {
        if (step === 'AMOUNT') formSubEl.textContent = tr('transfer_step_amount_sub', 'Confirm source account and amount.');
        else if (step === 'ACCOUNT') formSubEl.textContent = tr('transfer_step_account_sub', 'Select the account to send from.');
        else formSubEl.textContent = tr('transfer_step_recipient_sub', 'Select the recipient.');
    }
    if (selectedRecipientNameEl) selectedRecipientNameEl.textContent = selectedRecipient ? selectedRecipient.name : '-';
    if (selectedRecipientAddressEl) selectedRecipientAddressEl.textContent = selectedRecipient ? selectedRecipient.address : '-';
    if (selectedAccountNameEl) selectedAccountNameEl.textContent = selectedAccount ? `${selectedAccount.name} · ${selectedAccount.number}` : '-';

    if (accountListEl) {
        accountListEl.innerHTML = state.remittance.accounts.map(account => {
            const active = account.id === state.remittance.selectedAccountId;
            const cls = active
                ? 'border-brand bg-brand-soft text-brand'
                : 'border-slate-200 bg-white text-slate-700';
            return `
                <button type="button" onclick="selectTransferAccount('${account.id}')"
                    class="w-full text-left rounded-2xl border px-4 py-3 ${cls}">
                    <p class="text-xs font-black">${account.name}</p>
                    <p class="text-[11px] font-black mt-1">${account.number} · ${formatKRW(account.balance)}</p>
                </button>
            `;
        }).join('');
    }

    if (recipientListEl) {
        recipientListEl.innerHTML = state.remittance.recipients.map(recipient => {
            const active = recipient.id === state.remittance.selectedRecipientId;
            const cls = active
                ? 'border-brand bg-brand-soft'
                : 'border-slate-200 bg-white';
            return `
                <button type="button" onclick="selectTransferRecipient('${recipient.id}')"
                    class="w-full rounded-2xl border px-4 py-3 text-left ${cls}">
                    <p class="text-xs font-black text-slate-900">${recipient.name}</p>
                    <p class="text-[11px] text-slate-500 font-mono mt-1 break-all">${recipient.address}</p>
                </button>
            `;
        }).join('');
    }

    if (amountInputEl) {
        if (Number(amountInputEl.value) !== Number(state.remittance.draftAmount)) {
            amountInputEl.value = String(state.remittance.draftAmount || 0);
        }
    }
    if (amountHintEl) {
        const accountText = selectedAccount ? `${selectedAccount.name}` : '-';
        const recipientText = selectedRecipient ? selectedRecipient.name : '-';
        amountHintEl.textContent = `${accountText} → ${recipientText}`;
    }
}

function renderTransferConfirm() {
    const selected = getSelectedRecipient();
    const account = getSelectedAccount();
    const amount = Math.max(0, Number(state.remittance.draftAmount || 0));
    const accountEl = document.getElementById('send-confirm-account');
    const recipientEl = document.getElementById('send-confirm-recipient');
    const addressEl = document.getElementById('send-confirm-address');
    const amountEl = document.getElementById('send-confirm-amount');

    if (accountEl) accountEl.textContent = account ? `${account.name} · ${account.number}` : '-';
    if (recipientEl) recipientEl.textContent = selected ? selected.name : '-';
    if (addressEl) addressEl.textContent = selected ? selected.address : '-';
    if (amountEl) amountEl.textContent = `${amount} USDC`;
}

function openTransferConfirm() {
    const selected = getSelectedRecipient();
    if (!selected) return;
    const selectedAccount = getSelectedAccount();
    if (!selectedAccount) return;

    const amount = Number(state.remittance.draftAmount || 0);
    if (!Number.isFinite(amount) || amount <= 0) {
        showToast(tr('toast_enter_amount', 'Please enter an amount.'));
        return;
    }
    if (amount > selectedAccount.balance) {
        showToast(tr('toast_insufficient_balance', 'Insufficient balance in the selected account.'));
        return;
    }

    renderTransferConfirm();
    openOverlay('overlay-send-confirm');
}

function confirmTransfer() {
    closeOverlay('overlay-send-confirm');
    closeTransferPage();
    startTransfer();
}

function startTransfer() {
    const transferAmount = Math.max(0, Math.floor(Number(state.remittance.draftAmount || 0)));
    const transferAccountId = state.remittance.selectedAccountId;
    openOverlay('transfer-tracker');
    state.remittance.status = 'SUBMITTED';
    renderHome();

    const submittedIcon = document.getElementById('step-submitted-icon');
    const confirmedIcon = document.getElementById('step-confirmed-icon');
    const confirmedSub = document.getElementById('transfer-confirmed-sub');
    const submittedSub = document.getElementById('transfer-submitted-sub');
    if (submittedSub) submittedSub.textContent = tr('transfer_submitted_to_network', 'Submitted to network');
    if (confirmedSub) confirmedSub.textContent = tr('transfer_confirming', 'Confirming...');
    if (submittedIcon) {
        submittedIcon.className = 'w-12 h-12 rounded-full bg-emerald-500 flex items-center justify-center text-white shadow-lg shadow-emerald-100';
        submittedIcon.innerHTML = '<i class="fa-solid fa-check text-lg"></i>';
    }
    if (confirmedIcon) {
        confirmedIcon.className = 'w-12 h-12 rounded-full bg-brand-soft flex items-center justify-center text-brand';
        confirmedIcon.innerHTML = '<div class="w-2 h-2 bg-brand rounded-full animate-ping"></div>';
    }

    window.setTimeout(() => {
        state.remittance.status = 'CONFIRMED';
        const transferAccount = state.remittance.accounts.find(a => a.id === transferAccountId);
        if (transferAccount && transferAmount > 0) {
            transferAccount.balance = Math.max(0, transferAccount.balance - transferAmount);
        }
        if (confirmedIcon) {
            confirmedIcon.className = 'w-12 h-12 rounded-full bg-emerald-500 flex items-center justify-center text-white shadow-lg shadow-emerald-100';
            confirmedIcon.innerHTML = '<i class="fa-solid fa-check text-lg"></i>';
        }
        if (confirmedSub) confirmedSub.textContent = tr('transfer_confirmed_sub', 'Confirmed');

        const receipt = state.docs.find(d => d.type === 'TRANSFER_RECEIPT');
        if (receipt) {
            receipt.status = 'READY';
            receipt.updatedAt = `${formatDate(state.demo.asOfDay)} 16:10`;
        }
        renderDocs();
        renderSend();
        renderHome();
    }, 1400);
}

function syncDemoModeUI() {
    const controls = document.getElementById('demo-controls');
    const btn = document.getElementById('btn-demo-toggle');
    if (controls) controls.classList.toggle('hidden', !state.demo.enabled);
    if (btn) btn.textContent = state.demo.enabled ? 'ON' : 'OFF';
}

function toggleDemoMode() {
    state.demo.enabled = !state.demo.enabled;
    if (!state.demo.enabled && state.demo.autoplay) toggleAutoPlay();
    syncDemoModeUI();
}

function setAsOfDay(day) {
    state.demo.asOfDay = day;
    const slider = document.getElementById('asof-slider');
    if (slider && Number(slider.value) !== day) slider.value = String(day);
    renderAll();
}

function toggleAutoPlay() {
    state.demo.autoplay = !state.demo.autoplay;
    const btn = document.getElementById('btn-autoplay');
    if (btn) btn.innerHTML = state.demo.autoplay ? '<i class="fa-solid fa-pause"></i>' : '<i class="fa-solid fa-play"></i>';

    if (state.demo.autoplay) {
        state.demo.autoplayTimer = window.setInterval(() => {
            const next = state.demo.asOfDay >= 31 ? 1 : state.demo.asOfDay + 1;
            setAsOfDay(next);
        }, 900);
    } else {
        if (state.demo.autoplayTimer) window.clearInterval(state.demo.autoplayTimer);
        state.demo.autoplayTimer = null;
    }
}

// Init
setLang('ko');
syncDemoModeUI();
setHeaderForTab(Route.HOME);
renderAll();

document.getElementById('asof-slider')?.addEventListener('input', (e) => setAsOfDay(Number(e.target.value)));
document.querySelectorAll('[data-lang]').forEach(btn => btn.addEventListener('click', () => setLang(btn.getAttribute('data-lang'))));
document.getElementById('onboarding-wage-amount')?.addEventListener('input', renderOnboardingHourlyPreview);
document.getElementById('onboarding-unit-hours')?.addEventListener('input', renderOnboardingHourlyPreview);
