(function initMockupDomain(global) {
    function createDomain({ state, translations }) {
        function formatDate(day) {
            const mm = String(state.demo.month).padStart(2, '0');
            const dd = String(day).padStart(2, '0');
            return `${state.demo.year}-${mm}-${dd}`;
        }

        function formatKRW(value) {
            const n = Number(value || 0);
            return '₩' + n.toLocaleString('ko-KR');
        }

        function getLang() {
            return localStorage.getItem('wppLang') || 'ko';
        }

        function getDict() {
            return translations[getLang()] || translations.ko;
        }

        function tr(key, fallback) {
            const dict = getDict();
            return dict[key] || fallback || key;
        }

        function computeWageEstimate() {
            const w = state.wage;
            const base = w.hourly * w.totalHours;
            const otPremium = w.hourly * w.overtimeHours * 0.5;
            const nightPremium = w.hourly * w.nightHours * 0.5;
            const total = base + otPremium + nightPremium;
            return { base, otPremium, nightPremium, total };
        }

        function parseHourToMinutes(timeText) {
            if (!timeText || !timeText.includes(':')) return null;
            const [h, m] = timeText.split(':').map(Number);
            if (!Number.isFinite(h) || !Number.isFinite(m)) return null;
            return h * 60 + m;
        }

        function shiftYearMonth(year, month, delta) {
            const total = year * 12 + (month - 1) + delta;
            return {
                year: Math.floor(total / 12),
                month: (total % 12) + 1
            };
        }

        function getWorkproofCalendarContext() {
            return shiftYearMonth(state.demo.year, state.demo.month, state.workproof.calendarOffset || 0);
        }

        function getDaysInMonth(year, month) {
            return new Date(year, month, 0).getDate();
        }

        function getWorkproofMonthRecords(year, month) {
            if (year !== state.demo.year || month !== state.demo.month) return [];

            const asOf = state.demo.asOfDay;
            const base = state.workproof.records
                .filter(r => r.day <= asOf)
                .map(r => ({ ...r }));

            const today = state.workproof.today;
            const hasTodayRecord = !!today.clockIn || !!today.clockOut;
            if (hasTodayRecord && !base.some(r => r.day === asOf)) {
                base.push({
                    id: `WP-TODAY-${asOf}`,
                    day: asOf,
                    inTime: today.clockIn || '-',
                    outTime: today.clockOut || '-',
                    modified: false,
                    reasonKey: null,
                    reasonMemo: null,
                    attachments: 0,
                    synthetic: true
                });
            }

            return base.sort((a, b) => b.day - a.day);
        }

        function getWorkproofLevel(record) {
            if (!record) return 0;
            if (record.modified) return 3;
            const hasIn = !!record.inTime && record.inTime !== '-';
            const hasOut = !!record.outTime && record.outTime !== '-';
            if (hasIn && hasOut) return 2;
            if (hasIn) return 1;
            return 0;
        }

        function getWorkproofStatusKey(level) {
            if (level === 3) return 'calendar_status_edited';
            if (level === 2) return 'calendar_status_done';
            if (level === 1) return 'calendar_status_pending';
            return 'calendar_status_none';
        }

        function computeVerifiedWorkSnapshot() {
            const asOf = state.demo.asOfDay;
            const validRecords = state.workproof.records.filter(r => r.day <= asOf && r.inTime && r.outTime);

            let totalMinutes = 0;
            validRecords.forEach(r => {
                const inMin = parseHourToMinutes(r.inTime);
                const outMin = parseHourToMinutes(r.outTime);
                if (inMin === null || outMin === null || outMin <= inMin) return;
                totalMinutes += (outMin - inMin);
            });

            const recordHours = Number((totalMinutes / 60).toFixed(1));
            const verifiedDays = Math.max(validRecords.length, state.wage.workDays || 0);
            const verifiedHours = Math.max(recordHours, state.wage.totalHours || 0);
            const verifiedAmount = Math.floor((totalMinutes / 60) * state.wage.hourly);
            const summaryBasedAmount = Math.floor((state.wage.totalHours || 0) * state.wage.hourly);

            return {
                verifiedDays,
                verifiedHours,
                verifiedAmount: Math.max(verifiedAmount, summaryBasedAmount)
            };
        }

        function getAdvanceTier(verifiedDays) {
            if (verifiedDays >= 20) return { name: 'T3', cap: 500000 };
            if (verifiedDays >= 15) return { name: 'T2', cap: 400000 };
            if (verifiedDays >= 10) return { name: 'T1', cap: 300000 };
            return { name: 'T0', cap: 200000 };
        }

        function computeAdvanceSnapshot() {
            const verified = computeVerifiedWorkSnapshot();
            const tier = getAdvanceTier(verified.verifiedDays);
            const rawLimit = Math.floor(verified.verifiedAmount * state.advance.limitRate);
            const baseLimit = Math.min(rawLimit, tier.cap, state.advance.maxCap);
            const bonus = state.advance.previousRepaymentGood ? state.advance.bonusLimit : 0;
            const totalLimit = Math.min(state.advance.maxCap, baseLimit + bonus);
            const used = Math.min(totalLimit, Math.max(0, state.advance.used));
            const available = Math.max(0, totalLimit - used);
            const requestOptions = [100000, 200000, 300000, 500000];
            const maxSelectable = Math.floor(available / 100000) * 100000;

            if (state.advance.selectedRequest > maxSelectable) {
                state.advance.selectedRequest = maxSelectable >= 100000 ? maxSelectable : 100000;
            }

            const requestAmount = maxSelectable >= 100000
                ? Math.min(state.advance.selectedRequest, maxSelectable)
                : 0;
            const fee = requestAmount > 0 ? state.advance.flatFee : 0;
            const receive = Math.max(0, requestAmount - fee);
            const repayDate = `${state.demo.year}-${String(state.demo.month).padStart(2, '0')}-31`;

            return {
                verifiedDays: verified.verifiedDays,
                verifiedHours: verified.verifiedHours,
                verifiedAmount: verified.verifiedAmount,
                tierName: tier.name,
                tierCap: tier.cap,
                rawLimit,
                baseLimit,
                bonus,
                totalLimit,
                used,
                available,
                requestOptions,
                requestAmount,
                fee,
                receive,
                repayDate
            };
        }

        function computeVaultApplyAvailable() {
            const base = Math.floor((state.wage.actualDeposit || 0) * 0.35);
            return Math.max(100000, base);
        }

        function computeYieldSnapshot(depositAmount = state.vault.userDeposit) {
            const userDeposit = Math.max(0, depositAmount || 0);
            const poolTotal = Math.max(1, state.vault.totalPool);
            const defiMonthly = Math.floor(userDeposit * (state.vault.apr / 12));
            const userShare = userDeposit / poolTotal;
            const feeMonthly = Math.floor(state.vault.monthlyFeeRevenue * userShare);
            const monthlyTotal = defiMonthly + feeMonthly;
            const dailyTotal = Math.floor(monthlyTotal / 30);
            const advanceUsedAmount = Math.floor(poolTotal * state.vault.advanceRatio * state.vault.advanceUtilization);

            return {
                userDeposit,
                defiMonthly,
                feeMonthly,
                monthlyTotal,
                dailyTotal,
                poolTotal,
                defiRatio: state.vault.defiRatio,
                advanceRatio: state.vault.advanceRatio,
                advanceUtilization: state.vault.advanceUtilization,
                advanceUsedAmount
            };
        }

        return {
            formatDate,
            formatKRW,
            getLang,
            getDict,
            tr,
            computeWageEstimate,
            parseHourToMinutes,
            shiftYearMonth,
            getWorkproofCalendarContext,
            getDaysInMonth,
            getWorkproofMonthRecords,
            getWorkproofLevel,
            getWorkproofStatusKey,
            computeVerifiedWorkSnapshot,
            getAdvanceTier,
            computeAdvanceSnapshot,
            computeVaultApplyAvailable,
            computeYieldSnapshot
        };
    }

    global.MockupDomain = Object.freeze({
        create: createDomain
    });
})(window);
