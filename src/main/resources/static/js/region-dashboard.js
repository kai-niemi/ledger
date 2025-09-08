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
            stompClient.subscribe(_this.settings.topics.balanceSheetUpdate, function (payload) {
                var balanceSheet = JSON.parse(payload.body);
                _this.handleModelUpdate(balanceSheet);
            });
        });
    },

    getElement: function (id) {
        return $('#' + id);
    },

    handleModelUpdate: function (balanceSheet) {
        var _this = this;

        console.log("Model update: " + balanceSheet);

        var divElt = _this.getElement("city-" +  balanceSheet.city.name);

        // Flash spinner
        var _spinnerElt = divElt.find(".ledger-city-spinner");
        _spinnerElt.attr('style','display: block');
        setTimeout(function () {
            _spinnerElt.attr('style','display: none');
        }, 1500);

        divElt.find(".ledger-totalBalance").text(_this.formatMoney(balanceSheet.totalBalance));
        divElt.find(".ledger-totalChecksum").text(_this.formatMoney(balanceSheet.totalChecksum));
        divElt.find(".ledger-totalTurnover").text(_this.formatMoney(balanceSheet.totalTurnover));
        divElt.find(".ledger-numberOfAccounts").text(balanceSheet.numberOfAccounts);
        divElt.find(".ledger-numberOfTransfers").text(balanceSheet.numberOfTransfers);
        divElt.find(".ledger-last-active").text(balanceSheet.lastActive);
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
            balanceSheetUpdate: '/topic/balance-sheet/update',
        },
    });
});

