//<![CDATA[
const BookingUI = (function () {
    const state = {
        currentStep: 1,
        tableCount: 20,
        pricePerGuest: 1200000,
        serviceCharge: 5000000,
        addOns: [ { id: 1, name: 'Extra dessert table',   price: 50000,  unit: 'guest', quantity: 0 },
            { id: 2, name: 'Premium wine pairing',  price: 500000, unit: 'table', quantity: 0 },
            { id: 3, name: 'Late-night snacks',     price: 35000,  unit: 'guest', quantity: 0 }],
        depositPercentage: 30,
        discount: 0,
        voucherCode: '',
        eventType: 'wedding',
        serviceLevel: 'premium',
        locationType: 'AT_RESTAURANT',
        tax: 0,
        totalAmount: 0,
        depositAmount: 0,
        remainingAmount: 0
    };


    function formatNumber(n) {
        return n.toLocaleString('en-US');
    }

    function updateCapacity() {
        const totalGuests = state.tableCount * 10;
        const capacityEl = document.getElementById('capacity-status');
        const guestLabel = document.getElementById('guest-count-label');
        if (!capacityEl || !guestLabel)
            return;

        let color = '#22C55E';
        let message = 'Within capacity';
        if (state.tableCount >= 10 && state.tableCount <= 25) {
            color = '#22C55E';
            message = 'Ideal for 15–25 tables (150–250 guests)';
        } else if (state.tableCount > 25 && state.tableCount <= 40) {
            color = '#FACC15';
            message = 'Near maximum capacity';
        } else if (state.tableCount > 40) {
            color = '#EF4444';
            message = 'This venue supports 10–50 tables (100–500 guests)';
        }

        capacityEl.style.color = color;
        capacityEl.querySelector('span') && (capacityEl.querySelector('span').textContent = '');
        capacityEl.lastChild && (capacityEl.lastChild.textContent = message);
        guestLabel.textContent = '(~' + totalGuests + ' guests at 10 per table)';

        // summary table + guests
        const summaryLabel = document.getElementById('summary-table-guest');
        const packageLabel = document.getElementById('summary-package-label');
        if (summaryLabel) {
            summaryLabel.textContent = state.tableCount + ' tables (~' + totalGuests + ' guests)';
        }
        if (packageLabel) {
            packageLabel.textContent = 'Package (' + state.tableCount + ' tables, ~' + totalGuests + ' guests)';
        }
        const guestHidden = document.getElementById('hf-guest-count');
        if (guestHidden) {
            guestHidden.value = totalGuests;
        }

    }

    function collectAddonQuantities() {
        const rows = document.querySelectorAll('.addon-row');
        rows.forEach(row => {
            const id = parseInt(row.getAttribute('data-addon-id'), 10);
            const qtyEl = row.querySelector('.addon-qty');
            const qty = parseInt(qtyEl.textContent || '0', 10);
            const addon = state.addOns.find(a => a.id === id);
            if (addon)
                addon.quantity = qty;
        });
    }

    function updateSummary() {
        collectAddonQuantities();
        const totalGuests = state.tableCount * 10;
        const packageSubtotal = state.pricePerGuest * totalGuests;
        const addOnsSubtotal = state.addOns.reduce((sum, a) => sum + a.price * a.quantity, 0);
        const subtotal = packageSubtotal + addOnsSubtotal;
        const tax = Math.round((subtotal + state.serviceCharge) * 0.1);
        const discount = state.discount;
        const totalWithCharges = subtotal + state.serviceCharge + tax - discount;
        const depositAmount = Math.round(totalWithCharges * (state.depositPercentage / 100));
        const remainingAmount = totalWithCharges - depositAmount;

        // store tax/discount for later
        state.tax = tax;
        state.totalAmount = totalWithCharges;
        state.depositAmount = depositAmount;
        state.remainingAmount = remainingAmount;

// push to hidden fields (nếu tồn tại)
        const totalField = document.getElementById('hf-total-amount');
        if (totalField)
            totalField.value = totalWithCharges;

        const depositField = document.getElementById('hf-deposit-amount');
        if (depositField)
            depositField.value = depositAmount;

        const remainingField = document.getElementById('hf-remaining-amount');
        if (remainingField)
            remainingField.value = remainingAmount;


        // write to DOM
        const pkgTotalEl = document.getElementById('summary-package-total');
        if (pkgTotalEl)
            pkgTotalEl.textContent = formatNumber(packageSubtotal) + ' VND';

        const serviceEl = document.getElementById('summary-service-charge');
        if (serviceEl)
            serviceEl.textContent = formatNumber(state.serviceCharge) + ' VND';

        const taxEl = document.getElementById('summary-tax');
        if (taxEl)
            taxEl.textContent = formatNumber(tax) + ' VND';

        const discountRow = document.getElementById('summary-discount-row');
        const discountEl = document.getElementById('summary-discount');
        if (discountRow && discountEl) {
            if (discount > 0) {
                discountRow.classList.remove('hidden');
                discountEl.textContent = '-' + formatNumber(discount) + ' VND';
            } else {
                discountRow.classList.add('hidden');
            }
        }

        // add-ons rows in summary
        const container = document.getElementById('summary-addons-container');
        if (container) {
            container.innerHTML = '';
            state.addOns.filter(a => a.quantity > 0).forEach(addon => {
                const row = document.createElement('div');
                row.className = 'flex justify-between';
                row.innerHTML =
                        '<span class="text-[#4B5563]">' +
                        addon.name + ' (' + addon.quantity + ')</span>' +
                        '<span class="font-medium text-[#111827]">' +
                        formatNumber(addon.price * addon.quantity) + ' VND</span>';
                container.appendChild(row);
            });
        }

        const totalEl = document.getElementById('summary-total-amount');
        const depositEl = document.getElementById('summary-deposit');
        const remainingEl = document.getElementById('summary-remaining');
        if (totalEl)
            totalEl.textContent = formatNumber(totalWithCharges) + ' VND';
        if (depositEl)
            depositEl.textContent = formatNumber(depositAmount) + ' VND';
        if (remainingEl)
            remainingEl.textContent = formatNumber(remainingAmount) + ' VND';
    }

    function setServiceLevel(level) {
        state.serviceLevel = level;
        const buttons = document.querySelectorAll('.service-level-btn');
        buttons.forEach(btn => {
            const val = btn.getAttribute('data-level');
            if (val === level) {
                btn.className =
                        'service-level-btn px-4 py-3 rounded-xl font-medium text-sm transition-all duration-200 bg-[#020617] text-white border-2 border-[#D4AF37]';
            } else {
                btn.className =
                        'service-level-btn px-4 py-3 rounded-xl font-medium text-sm transition-all duration-200 bg-white text-[#4B5563] border-2 border-[#E5E7EB] hover:border-[#D4AF37]';
            }
        });
        const summary = document.getElementById('summary-service-level');
        if (summary)
            summary.textContent = level;
    }

    function setLocationType(type) {
        const btns = document.querySelectorAll('.location-btn');
        btns.forEach(btn => {
            const val = btn.getAttribute('data-location');
            if (val === type) {
                btn.className =
                        'location-btn p-6 rounded-xl border-2 transition-all duration-200 text-left border-[#D4AF37] bg-[#D4AF37]/5';
            } else {
                btn.className =
                        'location-btn p-6 rounded-xl border-2 transition-all duration-200 text-left border-[#E5E7EB] hover:border-[#D4AF37]';
            }
        });
        const outsideWrapper = document.getElementById('outside-address-wrapper');
        if (outsideWrapper) {
            if (type === 'outside') {
                outsideWrapper.classList.remove('hidden');
            } else {
                outsideWrapper.classList.add('hidden');
            }
        }

        state.locationType = (type === 'outside') ? 'OUTSIDE' : 'AT_RESTAURANT';

        const locHidden = document.getElementById('hf-location-type');
        if (locHidden) {
            locHidden.value = state.locationType;
        }

    }

    function setPaymentMethod(method) {
        const cards = document.querySelectorAll('.payment-method');
        cards.forEach(card => {
            const val = card.getAttribute('data-method');
            if (val === method) {
                card.className =
                        'payment-method p-6 rounded-xl border-2 transition-all duration-200 text-left relative border-[#D4AF37] bg-[#020617] text-white';
            } else {
                card.className =
                        'payment-method p-6 rounded-xl border-2 transition-all duration-200 text-left relative border-[#E5E7EB] bg-white hover:border-[#D4AF37]';
            }
        });
    }

    function setPaymentType(type) {
        state.paymentType = type;
        const buttons = document.querySelectorAll('.pay-type');
        buttons.forEach(btn => {
            const val = btn.getAttribute('data-type');
            const radioOuter = btn.querySelector('.radio-outer');
            const radioInner = btn.querySelector('.radio-inner');
            if (val === type) {
                btn.className =
                        'pay-type w-full p-6 rounded-xl border-2 transition-all duration-200 text-left border-[#D4AF37] bg-[#D4AF37]/5';
                if (radioOuter)
                    radioOuter.classList.add('border-[#D4AF37]');
                if (radioInner)
                    radioInner.classList.add('bg-[#D4AF37]');
            } else {
                btn.className =
                        'pay-type w-full p-6 rounded-xl border-2 transition-all duration-200 text-left border-[#E5E7EB] hover:border-[#D4AF37]';
                if (radioOuter)
                    radioOuter.classList.remove('border-[#D4AF37]');
                if (radioInner)
                    radioInner.classList.remove('bg-[#D4AF37]');
            }
        });
        // nếu trả full thì depositPercentage = 100, còn lại 30
        state.depositPercentage = (type === 'full') ? 100 : 30;
        updateSummary();
    }

    function goToStep(step) {
        state.currentStep = step;
        const s1 = document.getElementById('step-1');
        const s2 = document.getElementById('step-2');
        const s3 = document.getElementById('step-3');
        const sections = [s1, s2, s3];
        sections.forEach((sec, idx) => {
            if (!sec)
                return;
            const index = idx + 1;
            if (index === step) {
                sec.classList.remove('hidden', 'opacity-0');
                sec.classList.add('opacity-100');
            } else {
                sec.classList.add('hidden');
                sec.classList.remove('opacity-100');
            }
        });

        // Stepper UI
        const stepper = document.getElementById('stepper');
        if (!stepper)
            return;
        const circles = stepper.querySelectorAll('.step-circle');
        const titles = stepper.querySelectorAll('span.mt-3');
        const lines = stepper.querySelectorAll('.step-line');

        circles.forEach((c, idx) => {
            const index = idx + 1;
            if (index < step) {
                c.className =
                        'step-circle w-12 h-12 rounded-full flex items-center justify-center font-semibold text-base transition-all duration-300 relative bg-[#D4AF37] text-white shadow-lg shadow-[#D4AF37]/30';
                c.textContent = '✓';
            } else if (index === step) {
                c.className =
                        'step-circle w-12 h-12 rounded-full flex items-center justify-center font-semibold text-base transition-all duration-300 relative bg-[#020617] text-white ring-4 ring-[#D4AF37]/30 shadow-lg';
                c.textContent = String(index);
            } else {
                c.className =
                        'step-circle w-12 h-12 rounded-full flex items-center justify-center font-semibold text-base transition-all duration-300 relative bg-white text-[#9CA3AF] border-2 border-[#E5E7EB]';
                c.textContent = String(index);
            }
        });

        titles.forEach((t, idx) => {
            const index = idx + 1;
            if (index === step)
                t.classList.add('text-[#111827]');
            else
                t.classList.remove('text-[#111827]');
        });

        lines.forEach((line, idx) => {
            const srcIndex = idx + 1; // line between step srcIndex and srcIndex+1
            if (srcIndex < step) {
                line.classList.remove('step-line-zero');
                line.classList.add('step-line-full');
            } else {
                line.classList.remove('step-line-full');
                line.classList.add('step-line-zero');
            }
        });
    }

    function validateContact() {
        const full = document.getElementById('contact-fullname').value.trim();
        const email = document.getElementById('contact-email').value.trim();
        const phone = document.getElementById('contact-phone').value.trim();
        const accept = document.getElementById('accept-policies').checked;

        const emailError = document.getElementById('contact-email-error');
        const phoneError = document.getElementById('contact-phone-error');

        const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
        const phoneValid = /^[\d\s\-\+\(\)]+$/.test(phone) && phone.length >= 10;

        if (emailError)
            emailError.classList.toggle('hidden', emailValid || !email);
        if (phoneError)
            phoneError.classList.toggle('hidden', phoneValid || !phone);

        const canProceed = full && emailValid && phoneValid && accept;
        const nextBtn = document.getElementById('btn-step2-next');
        if (nextBtn)
            nextBtn.disabled = !canProceed;
    }

    function init() {
        // init lucide
        if (window.lucide) {
            window.lucide.createIcons();
        }

        // ---- Lấy tham số từ URL (restaurantId, date, slot) ----
        const params = new URLSearchParams(window.location.search);
        const restaurantId = params.get('restaurantId');
        const dateParam = params.get('date');  // dạng YYYY-MM-DD
        const slotParam = params.get('slot');  // vd: "Dinner (18:00–22:00)"

        // Format ngày cho hiển thị: 2025-11-19 -> 19 Nov 2025
        function formatDateDisplay(isoDate) {
            if (!isoDate)
                return '';
            const d = new Date(isoDate);
            if (isNaN(d.getTime())) {
                return isoDate; // nếu parse lỗi thì hiện raw
            }
            const day = String(d.getDate()).padStart(2, '0');
            const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            const month = monthNames[d.getMonth()];
            const year = d.getFullYear();
            return day + ' ' + month + ' ' + year;
        }

        // Nếu có date/slot truyền sang thì cập nhật UI
        if (dateParam || slotParam) {
            const eventDatetimeEl = document.getElementById('event-datetime');
            const summaryDateEl = document.getElementById('summary-event-date');
            const summarySlotEl = document.getElementById('summary-time-slot');

            const dateDisplay = dateParam ? formatDateDisplay(dateParam) : null;

            if (eventDatetimeEl) {
                if (dateDisplay && slotParam) {
                    eventDatetimeEl.textContent = dateDisplay + ' – ' + slotParam;
                } else if (dateDisplay) {
                    eventDatetimeEl.textContent = dateDisplay;
                } else if (slotParam) {
                    eventDatetimeEl.textContent = slotParam;
                }
            }

            if (summaryDateEl && dateDisplay) {
                summaryDateEl.textContent = dateDisplay;
            }

            if (summarySlotEl && slotParam) {
                summarySlotEl.textContent = slotParam;
            }
        }


        // table buttons
        const minusBtn = document.getElementById('btn-minus-table');
        const plusBtn = document.getElementById('btn-plus-table');
        const tableInput = document.getElementById('table-count-input');

        if (minusBtn && plusBtn && tableInput) {
            minusBtn.addEventListener('click', function () {
                let val = parseInt(tableInput.value || '1', 10);
                if (val > 1)
                    val--;
                tableInput.value = String(val);
                state.tableCount = val;
                updateCapacity();
                updateSummary();
            });
            plusBtn.addEventListener('click', function () {
                let val = parseInt(tableInput.value || '1', 10);
                val++;
                tableInput.value = String(val);
                state.tableCount = val;
                updateCapacity();
                updateSummary();
            });
            tableInput.addEventListener('input', function () {
                let val = parseInt(tableInput.value || '1', 10);
                if (isNaN(val) || val < 1)
                    val = 1;
                tableInput.value = String(val);
                state.tableCount = val;
                updateCapacity();
                updateSummary();
            });
        }

        // service level buttons
        document.querySelectorAll('.service-level-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const level = this.getAttribute('data-level');
                setServiceLevel(level);
            });
        });

        // location buttons
        document.querySelectorAll('.location-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const type = this.getAttribute('data-location');
                setLocationType(type);
            });
        });

        // addons
        document.querySelectorAll('.addon-row').forEach(row => {
            const minus = row.querySelector('.btn-addon-minus');
            const plus = row.querySelector('.btn-addon-plus');
            const qtyEl = row.querySelector('.addon-qty');

            function update(q) {
                qtyEl.textContent = String(q);
                if (minus)
                    minus.disabled = q === 0;
                updateSummary();
            }

            if (minus) {
                minus.addEventListener('click', function () {
                    let q = parseInt(qtyEl.textContent || '0', 10);
                    if (q > 0)
                        q--;
                    update(q);
                });
            }
            if (plus) {
                plus.addEventListener('click', function () {
                    let q = parseInt(qtyEl.textContent || '0', 10);
                    q++;
                    update(q);
                });
            }
            // init
            if (minus)
                minus.disabled = true;
        });

        // event type -> summary
        const eventTypeSelect = document.getElementById('event-type');
        if (eventTypeSelect) {
            eventTypeSelect.addEventListener('change', function () {
                state.eventType = this.value;
                const summary = document.getElementById('summary-event-type');
                if (summary)
                    summary.textContent = this.value;
            });
        }

        // tax invoice checkbox
        const needTax = document.getElementById('contact-need-tax');
        if (needTax) {
            needTax.addEventListener('change', function () {
                const box = document.getElementById('contact-need-tax-box');
                const detail = document.getElementById('tax-details');
                if (box) {
                    if (this.checked) {
                        box.classList.remove('bg-white', 'border-[#E5E7EB]');
                        box.classList.add('bg-[#D4AF37]', 'border-[#D4AF37]');
                    } else {
                        box.classList.add('bg-white', 'border-[#E5E7EB]');
                        box.classList.remove('bg-[#D4AF37]', 'border-[#D4AF37]');
                    }
                }
                if (detail) {
                    detail.classList.toggle('hidden', !this.checked);
                }
            });
        }

        // policies checkbox
        const accept = document.getElementById('accept-policies');
        if (accept) {
            accept.addEventListener('change', function () {
                const box = document.getElementById('accept-policies-box');
                if (box) {
                    if (this.checked) {
                        box.classList.remove('bg-white', 'border-[#E5E7EB]');
                        box.classList.add('bg-[#D4AF37]', 'border-[#D4AF37]');
                    } else {
                        box.classList.add('bg-white', 'border-[#E5E7EB]');
                        box.classList.remove('bg-[#D4AF37]', 'border-[#D4AF37]');
                    }
                }
                validateContact();
            });
        }

        // contact validation on input
        ['contact-fullname', 'contact-email', 'contact-phone'].forEach(id => {
            const el = document.getElementById(id);
            if (el)
                el.addEventListener('input', validateContact);
        });

        // step buttons
        const s1Next = document.getElementById('btn-step1-next');
        const s2Next = document.getElementById('btn-step2-next');
        const s2Back = document.getElementById('btn-step2-back');
        const s3Back = document.getElementById('btn-step3-back');

        if (s1Next)
            s1Next.addEventListener('click', function () {
                goToStep(2);
            });
        if (s2Next)
            s2Next.addEventListener('click', function () {
                goToStep(3);
            });
        if (s2Back)
            s2Back.addEventListener('click', function () {
                goToStep(1);
            });
        if (s3Back)
            s3Back.addEventListener('click', function () {
                goToStep(2);
            });

        // payment method cards
        document.querySelectorAll('.payment-method').forEach(card => {
            card.addEventListener('click', function () {
                const method = this.getAttribute('data-method');
                setPaymentMethod(method);
            });
        });

        // payment type
        document.querySelectorAll('.pay-type').forEach(btn => {
            btn.addEventListener('click', function () {
                const type = this.getAttribute('data-type');
                setPaymentType(type);
            });
        });

        // voucher
        const voucherInput = document.getElementById('voucher-input');
        const voucherBtn = document.getElementById('btn-apply-voucher');
        const voucherSuccess = document.getElementById('voucher-success');
        if (voucherBtn && voucherInput) {
            voucherBtn.addEventListener('click', function () {
                const code = voucherInput.value.trim().toUpperCase();
                state.voucherCode = code;
                if (code) {
                    state.discount = 2000000; // giống React
                    if (voucherSuccess)
                        voucherSuccess.classList.remove('hidden');
                } else {
                    state.discount = 0;
                    if (voucherSuccess)
                        voucherSuccess.classList.add('hidden');
                }
                updateSummary();
            });
        }

        // Confirm payment
        function handleConfirmClick() {
    const accept = document.getElementById('accept-policies');
    if (!accept || !accept.checked) {
        goToStep(2);
        validateContact();
        alert('Please fill your contact info and accept policies before payment.');
        return false;
    }

    validateContact();
    const nextBtn = document.getElementById('btn-step2-next');
    if (nextBtn && nextBtn.disabled) {
        goToStep(2);
        alert('Please complete your contact information before payment.');
        return false;
    }

    // Lấy param URL
    const params = new URLSearchParams(window.location.search);
    const restaurantIdParam = params.get('restaurantId');
    const dateParam = params.get('date');

    const setVal = (id, value) => {
        const el = document.getElementById(id);
        if (el) el.value = value != null ? value : '';
    };

    setVal('hf-restaurant-id', restaurantIdParam);
    setVal('hf-event-date', dateParam);
    setVal('hf-location-type', state.locationType);

    const street = document.getElementById('outside-street');
    const area = document.getElementById('outside-area');
    const city = document.getElementById('outside-city');
    const parts = [];
    if (street && street.value) parts.push(street.value);
    if (area && area.value) parts.push(area.value);
    if (city && city.value) parts.push(city.value);
    setVal('hf-outside-address', parts.join(', '));

    const totalGuests = state.tableCount * 10;
    setVal('hf-guest-count', totalGuests);
    setVal('hf-total-amount', state.totalAmount);
    setVal('hf-deposit-amount', state.depositAmount);
    setVal('hf-remaining-amount', state.remainingAmount);

    // cho submit form
    return true;
}


        // ---- Change venue: quay lại trang venue ----
        const changeVenueBtn = document.getElementById('btn-change-venue');
        if (changeVenueBtn) {
            changeVenueBtn.addEventListener('click', function () {
                // Nếu có restaurantId thì quay lại đúng trang chi tiết, không thì về list
                if (restaurantId) {
                    window.location.href = 'restaurant-details.xhtml?restaurantId=' + encodeURIComponent(restaurantId);
                } else {
                    window.location.href = 'restaurants.xhtml';
                }
            });
        }

        // ---- Change date & time: quay về Step 1 + scroll lên ----
        const changeDatetimeBtn = document.getElementById('btn-change-datetime');
        if (changeDatetimeBtn) {
            changeDatetimeBtn.addEventListener('click', function () {
                BookingUI.goToStep(1);  // về step 1
                const step1 = document.getElementById('step-1');
                if (step1) {
                    step1.scrollIntoView({behavior: 'smooth', block: 'start'});
                }
            });
        }

        // initial render
        updateCapacity();
        updateSummary();
        validateContact();
        setLocationType('restaurant');
        setServiceLevel('premium');
        setPaymentMethod('card');
        setPaymentType('deposit');
        goToStep(1);
    }

    return {
        init: init,
        goToStep: goToStep,
        handleConfirmClick: handleConfirmClick
    };
})();

document.addEventListener('DOMContentLoaded', function () {
    BookingUI.init();
});
//]]>