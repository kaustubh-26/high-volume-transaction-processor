(function () {
    if (window.__FETCH_INTERCEPTOR_INSTALLED__) {
        return;
    }
    window.__FETCH_INTERCEPTOR_INSTALLED__ = true;

    console.log("Swagger custom interceptor loaded");

    const originalFetch = window.fetch;

    function base64Encode(str) {
        return btoa(unescape(encodeURIComponent(str)));
    }

    function updateHeaderInputs() {
        try {
            const timestamp = new Date().toISOString().replace('.000Z', 'Z');
            const nonce = crypto.randomUUID();
            const idemKey = `idem-${crypto.randomUUID()}`;
            const correlationId = `corr-${crypto.randomUUID()}`;

            // Helper to find input by label text
            function setInput(labelText, value) {
                const labels = document.querySelectorAll("label");

                for (const label of labels) {
                    if (label.innerText.includes(labelText)) {
                        const input = label.closest("tr")?.querySelector("input");

                        if (input) {
                            const nativeSetter = Object.getOwnPropertyDescriptor(
                                window.HTMLInputElement.prototype,
                                "value"
                            ).set;

                            nativeSetter.call(input, value);

                            input.dispatchEvent(new Event("input", { bubbles: true }));
                            input.dispatchEvent(new Event("change", { bubbles: true }));
                        }
                    }
                }
            }

            setInput("X-Timestamp", timestamp);
            setInput("X-Nonce", nonce);
            setInput("Idempotency-Key", idemKey);
            setInput("X-Correlation-Id", correlationId);

            console.log("UI headers updated ✨");
        } catch (e) {
            console.error("UI update failed", e);
        }
    }

    document.addEventListener("click", function (e) {
        if (e.target && e.target.innerText === "Execute") {
            updateHeaderInputs();
        }
    });

    window.fetch = async function (url, options) {
        try {
            if (options && options.method === "POST" && url.includes("/api/")) {

                const saltKey = "merchant-demo-secret-key";
                const saltIndex = "1";
                const merchantId = "merchant-demo";

                const body = options.body || "";
                const fullUrl = url.startsWith("http")
                    ? url
                    : window.location.origin + url;

                const path = new URL(fullUrl).pathname;

                const timestamp = new Date().toISOString().replace('.000Z', 'Z');
                const nonce = crypto.randomUUID();
                const idemKey = `idem-${crypto.randomUUID()}`;
                const correlationId = `corr-${crypto.randomUUID()}`;

                const base64Body = base64Encode(body);

                const raw = `${base64Body}${path}${timestamp}${nonce}${idemKey}${saltKey}`;

                const encoder = new TextEncoder();
                const data = encoder.encode(raw);
                const hashBuffer = await crypto.subtle.digest("SHA-256", data);

                const hashHex = Array.from(new Uint8Array(hashBuffer))
                    .map(b => b.toString(16).padStart(2, '0'))
                    .join('')
                    .toUpperCase();

                const xVerify = `${hashHex}###${saltIndex}`;

                options.headers = options.headers || {};

                options.headers["X-Merchant-Id"] = merchantId;
                options.headers["X-Timestamp"] = timestamp;
                options.headers["X-Nonce"] = nonce;
                options.headers["X-Verify"] = xVerify;
                options.headers["Idempotency-Key"] = idemKey;
                options.headers["X-Correlation-Id"] = correlationId;

                console.log("Headers injected", options.headers);
            }
        } catch (e) {
            console.error("Interceptor error", e);
        }

        return originalFetch(url, options);
    };
})();