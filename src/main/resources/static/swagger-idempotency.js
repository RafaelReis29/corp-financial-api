(() => {
    const versions = ["1.0.0", "1.0.1", "2.0.0"];
    const idempotentVersion = "2.0.0";
    const versionHeader = "X-API-Version";
    const idempotencyHeader = "X-Idempotency-Key";
    const logoUrl = new URL("corp-api-logo.png", document.currentScript.src).href;

    function createIdempotencyUuid() {
        if (typeof crypto.randomUUID === "function") {
            return crypto.randomUUID();
        }

        const bytes = crypto.getRandomValues(new Uint8Array(16));
        bytes[6] = (bytes[6] & 0x0f) | 0x40;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;
        const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, "0")).join("");
        return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join("-");
    }

    function isHeader(parameter, name) {
        return parameter.get("in") === "header" && parameter.get("name") === name;
    }

    function ensureIdempotencyKey(system, pathMethod, parameter) {
        const value = system.specSelectors.parameterWithMetaByIdentity(pathMethod, parameter).get("value");
        if (!value) {
            system.specActions.changeParamByIdentity(pathMethod, parameter, createIdempotencyUuid());
        }
    }

    window.corpSwaggerRequestInterceptor = request => {
        if (!request.headers) {
            return request;
        }
        const versionName = Object.keys(request.headers).find(name => name.toLowerCase() === versionHeader.toLowerCase());
        if (versionName && request.headers[versionName] !== idempotentVersion) {
            const keyName = Object.keys(request.headers).find(name => name.toLowerCase() === idempotencyHeader.toLowerCase());
            if (keyName) {
                delete request.headers[keyName];
            }
        }
        return request;
    };

    window.corpIdempotencyPlugin = () => {
        let defaultVersion = idempotentVersion;
        const overrides = new Map();
        const listeners = new Set();

        const subscribe = listener => {
            listeners.add(listener);
            return () => listeners.delete(listener);
        };
        const notify = () => listeners.forEach(listener => listener());
        const selectedVersion = pathMethod => overrides.get(pathMethod.join(" ")) ?? defaultVersion;

        function versionOptions(React) {
            return versions.map(version => React.createElement("option", { key: version, value: version }, version));
        }

        return {
            wrapComponents: {
                Topbar: (Original, { React }) => () => {
                    const value = React.useSyncExternalStore(subscribe, () => defaultVersion);
                    return React.createElement("header", { className: "topbar" },
                        React.createElement("div", { className: "wrapper topbar-wrapper" },
                            React.createElement("div", { className: "corp-swagger-brand" },
                                React.createElement("img", { src: logoUrl, alt: "", width: 46, height: 46 }),
                                React.createElement("span", null, "Corp Financial API")),
                            React.createElement("label", { className: "corp-swagger-version" },
                                "Versão padrão",
                                React.createElement("select", {
                                    value,
                                    onChange: event => {
                                        defaultVersion = event.target.value;
                                        notify();
                                    }
                                }, versionOptions(React)))));
                },
                parameterRow: (Original, system) => {
                    const { React } = system;

                    function VersionRow(props) {
                        const version = React.useSyncExternalStore(subscribe, () => selectedVersion(props.pathMethod));
                        return React.createElement("tr", { "data-param-name": versionHeader, "data-param-in": "header" },
                            React.createElement("td", { className: "parameters-col_name" },
                                React.createElement("div", { className: "parameter__name" }, versionHeader),
                                React.createElement("div", { className: "parameter__in" }, "(header)")),
                            React.createElement("td", { className: "parameters-col_description" },
                                React.createElement("p", null, props.rawParam.get("description")),
                                React.createElement("select", {
                                    "aria-label": versionHeader,
                                    value: version,
                                    disabled: !props.isExecute,
                                    onChange: event => {
                                        overrides.set(props.pathMethod.join(" "), event.target.value);
                                        notify();
                                    }
                                }, versionOptions(React))));
                    }

                    function IdempotencyRow(props) {
                        const version = React.useSyncExternalStore(subscribe, () => selectedVersion(props.pathMethod));
                        React.useEffect(() => {
                            if (version === idempotentVersion && props.isExecute) {
                                ensureIdempotencyKey(system, props.pathMethod, props.rawParam);
                            }
                        }, [version, props.isExecute]);

                        return version === idempotentVersion
                            ? React.createElement(Original, { ...props, param: props.param.set("required", true) })
                            : null;
                    }

                    return props => {
                        if (isHeader(props.rawParam, versionHeader)) {
                            return React.createElement(VersionRow, props);
                        }
                        if (isHeader(props.rawParam, idempotencyHeader)) {
                            return React.createElement(IdempotencyRow, props);
                        }
                        return React.createElement(Original, props);
                    };
                }
            },
            statePlugins: {
                spec: {
                    wrapActions: {
                        execute: (original, system) => argumentsForRequest => {
                            const { path, method, operation } = argumentsForRequest;
                            const pathMethod = [path, method];
                            const parameters = operation.get("parameters");
                            const versionParameter = parameters?.find(parameter => isHeader(parameter, versionHeader));
                            const version = selectedVersion(pathMethod);
                            if (versionParameter) {
                                system.specActions.changeParamByIdentity(pathMethod, versionParameter, version);
                            }

                            const keyParameter = parameters?.find(parameter => isHeader(parameter, idempotencyHeader));
                            if (version === idempotentVersion && keyParameter) {
                                ensureIdempotencyKey(system, pathMethod, keyParameter);
                            }
                            return original(argumentsForRequest);
                        }
                    }
                }
            }
        };
    };
})();
