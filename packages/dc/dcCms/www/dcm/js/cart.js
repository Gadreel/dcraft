/* ************************************************************************
#
#  designCraft.io
#
#  https://designcraft.io/
#
#  Copyright:
#    Copyright 2015 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */

if (!dc.cms)
	dc.cms = {};

dc.cms.cart = {
	Order: {
		convertStatus: function(status) {
			if ((status == 'Pending') || (status == 'Completed') || (status == 'Canceled'))
				return status;
			
			return 'Processing';
		}
	},
	Cart: {
		_isLoaded: false,
		_cart: {
			Items: [],
			CustomerInfo: null,
			ShippingInfo: null,
			BillingInfo: null,
			Comment: null,
			Delivery: 'Ship',
			CouponCode: null,
			Coupon: null,				// coupon to apply to order (max 1 coupon)
			CalcInfo: {
				ItemCalc: 0,				// total of all items in cart  
				ProductDiscount: 0,			// discount toward product
				ItemTotal: 0,					// billing subtotal after product discount
				
				ShipCalc: 0,				// amount to base shipping off of, if based on $
				ShipAmount: 0,				// shipping charges
				ShipDiscount: 0,			// discount toward shipping
				ShipTotal: 0,				// amount to charge for shipping - do not include in taxes
				
				TaxCalc: 0,					// amount to base taxes off of, no shipping and no donations
				TaxAt: 0,
				TaxTotal: 0,				// amount to charge for taxes
				
				GrandTotal: 0				// final amount to charge
			}
		},
		
		empty: function() {
			this._cart = {
				Items: [],
				CustomerInfo: null,
				ShippingInfo: null,
				BillingInfo: null,
				Comment: null,
				Delivery: 'Ship',
				CouponCode: null,
				Coupon: null,				// coupon to apply to order (max 1 coupon)
				CalcInfo: {
					ItemCalc: 0,				// total of all items in cart  
					ProductDiscount: 0,			// discount toward product
					ItemTotal: 0,					// billing subtotal after product discount
					
					ShipCalc: 0,				// amount to base shipping off of, if based on $
					ShipAmount: 0,				// shipping charges
					ShipDiscount: 0,			// discount toward shipping
					ShipTotal: 0,				// amount to charge for shipping - do not include in taxes
					
					TaxCalc: 0,					// amount to base taxes off of, no shipping and no donations
					TaxAt: 0,
					TaxTotal: 0,				// amount to charge for taxes
					
					GrandTotal: 0				// final amount to charge
				}
			};
			
			return this._cart;
		},
		
		addItem: function(item) {
			if (!item.EntryId)
				item.EntryId = dc.util.Uuid.create();
			
			this._cart.Items.push(item);
			this.updateTotals();
		},

		removeItem: function(item) {
			for (var i = 0; i < this._cart.Items.length; i++) {
				if (this._cart.Items[i] == item) {
					this._cart.Items.splice(i, 1);
					this.updateTotals();
					return;
				}
			}
		},
		
		lookupItemById: function(id) {
			for (var i = 0; i < this._cart.Items.length; i++) {
				if (this._cart.Items[i].EntryId == id)
					return this._cart.Items[i];
			}
			
			return null;
		},		
		
		lookupItemByProduct: function(id) {
			for (var i = 0; i < this._cart.Items.length; i++) {
				if (this._cart.Items[i].Product == id)
					return this._cart.Items[i];
			}
			
			return null;
		},		
		
		lookupItemBySku: function(sku) {
			for (var i = 0; i < this._cart.Items.length; i++) {
				if (this._cart.Items[i].Sku == sku)
					return this._cart.Items[i];
			}
			
			return null;
		},		
		
		// assumes that estimated Shipping and Tax has been filled in by caller (when appropriate)
		updateTotals: function() {
			var carttotal = 0;
					
			for (var iidx = 0; iidx < this._cart.Items.length; iidx++) {
				var itm = this._cart.Items[iidx];
				
				var cost = (itm.SalePrice)
					? itm.Quantity * itm.SalePrice
					: itm.Quantity * itm.Price;
				
				itm.Total = String.formatMoney(cost) - 0;
				
				carttotal += itm.Total;
			}
			
			this._cart.CalcInfo.ItemCalc = this._cart.CalcInfo.ItemTotal = String.formatMoney(carttotal) - 0;
		},

		/*
		checkTotals: function() {
			dc.comm.sendTestMessage({Service: 'bw', Feature: 'Users', Op: 'CalcOrder', Body: this._cart });
		},
		*/
		
		load: function() {
			// load once per page
			if (this._isLoaded)
				return;

			this.empty();
				
			// load from localStorage
			try {
				var passphrase = dc.util.Cookies.getCookie("cinfo.phrase");				
				var crypted = sessionStorage.getItem("ws.cart");
				
				if (passphrase && crypted) {
					var plain = dc.util.Crypto.decrypt(crypted, passphrase);
					this._cart = JSON.parse(plain);
				}
			}
			catch (x) {
			}
			
			this._isLoaded = true;
			
			return this._cart;
		},
		
		// store the cart info temporarily, used from page to page during session
		save: function() {
			try {
				var passphrase = dc.util.Cookies.getCookie("cinfo.phrase");
				
				if (!passphrase) {
					passphrase = dc.util.Crypto.makeSimpleKey();
					dc.util.Cookies.setCookie("cinfo.phrase", passphrase, null, '/');   
				}
				
				var plain = JSON.stringify( this._cart );
				var crypted = dc.util.Crypto.encrypt(plain, passphrase);
				sessionStorage.setItem("ws.cart", crypted);
			}
			catch (x) {
			}
		},
		
		// store the cart info temporarily, used from page to page during session
		clear: function() {
			this.empty();
			
			try {
				sessionStorage.removeItem("ws.cart");
			}
			catch (x) {
			}
		}

	}
};