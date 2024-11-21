const RegionDashboard = function (settings) {
    this.settings = settings;
    this.init();
};

RegionDashboard.prototype = {
    init: function () {
        var socket = new SockJS(this.settings.endpoints.socket),
            stompClient = Stomp.over(socket),
            _this = this;

        // stompClient.log = (log) => {};
        stompClient.connect({}, function (frame) {
            stompClient.subscribe(_this.settings.topics.cityModelUpdate, function (payload) {
                var cityModel = JSON.parse(payload.body);
                _this.handleCityModelUpdate(cityModel);
            });
        });
    },

    getElement: function (id) {
        return $('#' + id);
    },

    handleCityModelUpdate: function (cityModel) {
        var _this = this;

        // console.log("City model update: " + cityModel);

        var divElt = _this.getElement("city-" +  cityModel.name);

        // Flash spinner
        var _spinnerElt = divElt.find(".ledger-city-spinner");
        _spinnerElt.attr('style','display: block');
        setTimeout(function () {
            _spinnerElt.attr('style','display: none');
        }, 1500);

        divElt.find(".ledger-totalBalance").text(_this.formatMoney(cityModel.totalBalance));
        divElt.find(".ledger-totalTurnover").text(_this.formatMoney(cityModel.totalTurnover));
        divElt.find(".ledger-numberOfAccounts").text(cityModel.numberOfAccounts);
        divElt.find(".ledger-numberOfTransfers").text(cityModel.numberOfTransfers);
        divElt.find(".ledger-last-active").text(cityModel.lastActive);
    },

    formatMoney: function (money) {
        var formatter = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: money.currency,
        });
        return formatter.format(money.amount);
    },
};

document.addEventListener('DOMContentLoaded', function () {
    new RegionDashboard({
        endpoints: {
            socket: '/ledger-service',
        },

        topics: {
            cityModelUpdate: '/topic/region/city/update',
        },
    });
});

