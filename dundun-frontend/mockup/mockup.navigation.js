(function initMockupNavigation(global) {
    const DEFAULT_OVERLAY_IDS = Object.freeze([
        'transfer-tracker',
        'overlay-send-confirm',
        'overlay-copilot',
        'overlay-claim',
        'overlay-workproof-day',
        'overlay-workproof-edit',
        'overlay-advance',
        'overlay-interest',
        'overlay-settings'
    ]);

    function createNavigation({ state, mainTabs, Route, overlayIds = DEFAULT_OVERLAY_IDS }) {
        function closeAllOverlays() {
            overlayIds.forEach(id => {
                const el = document.getElementById(id);
                if (el) el.classList.add('hidden');
            });
        }

        function openOverlay(id) {
            const el = document.getElementById(id);
            if (!el) return;
            el.classList.remove('hidden');
        }

        function closeOverlay(id) {
            const el = document.getElementById(id);
            if (!el) return;
            el.classList.add('hidden');
        }

        function getOpenLayers() {
            return overlayIds
                .map(id => document.getElementById(id))
                .filter(el => el && !el.classList.contains('hidden'));
        }

        function setActiveNav(tabId) {
            const navTabId = (tabId === Route.WAGE || tabId === Route.TRANSFER || tabId === Route.ACCOUNT_MANAGE)
                ? Route.FINANCE
                : tabId;
            document.querySelectorAll('.nav-btn').forEach(btn => {
                btn.classList.remove('text-brand');
                btn.classList.add('text-slate-400');
            });
            const activeBtn = document.getElementById('btn-' + navTabId);
            if (activeBtn) {
                activeBtn.classList.add('text-brand');
                activeBtn.classList.remove('text-slate-400');
            }
        }

        function setHeaderForTab(tabId) {
            const homeHeader = document.getElementById('home-header');
            if (!homeHeader) return;
            homeHeader.classList.toggle('hidden', tabId !== Route.HOME);
        }

        function setBottomNavForTab(tabId) {
            const nav = document.getElementById('bottom-nav');
            if (!nav) return;
            nav.classList.toggle('hidden', tabId === Route.TRANSFER || tabId === Route.ACCOUNT_MANAGE);
        }

        function switchTab(tabId) {
            closeAllOverlays();
            document.getElementById('screen-scroll')?.scrollTo({ top: 0, behavior: 'smooth' });

            mainTabs.forEach(id => {
                const tab = document.getElementById(id);
                if (!tab) return;
                tab.classList.remove('active');
                tab.classList.add('hidden');
            });

            const target = document.getElementById(tabId);
            if (target) {
                target.classList.remove('hidden');
                target.classList.add('active');
            }

            setActiveNav(tabId);
            setHeaderForTab(tabId);
            setBottomNavForTab(tabId);
            state.ui.currentTab = tabId;
        }

        return {
            closeAllOverlays,
            openOverlay,
            closeOverlay,
            getOpenLayers,
            setActiveNav,
            setHeaderForTab,
            setBottomNavForTab,
            switchTab
        };
    }

    global.MockupNavigation = Object.freeze({
        create: createNavigation,
        overlayIds: DEFAULT_OVERLAY_IDS
    });
})(window);
