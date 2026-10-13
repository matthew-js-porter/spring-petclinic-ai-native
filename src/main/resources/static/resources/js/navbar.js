/*
 * Collapsing navbar toggle: the only script in the app.
 * Written without arrow functions or other newer syntax so HtmlUnit's engine runs it in the page tests.
 * Without it the menu stays open (the collapsed state applies only under the pc-js class).
 */
(function () {
	var toggle = document.querySelector('.pc-navbar-toggle');
	var menu = toggle && document.getElementById(toggle.getAttribute('aria-controls'));
	if (!menu) {
		return;
	}
	document.documentElement.classList.add('pc-js');

	function setOpen(open) {
		toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
		menu.classList.toggle('pc-open', open);
	}

	toggle.addEventListener('click', function () {
		setOpen(toggle.getAttribute('aria-expanded') !== 'true');
	});

	document.addEventListener('keydown', function (event) {
		if ((event.key === 'Escape' || event.key === 'Esc') && toggle.getAttribute('aria-expanded') === 'true') {
			setOpen(false);
			toggle.focus();
		}
	});
})();
