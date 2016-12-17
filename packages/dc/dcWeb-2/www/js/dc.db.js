// dc.pui.Loader.addExtraLibs( ['/js/dc.db.js'] );

if (!dc.db)
	dc.db = {};

dc.db.database = {
	Ping: function() {
		dc.comm.sendMessage({ 
			Service: 'dcCoreDataServices', 
			Feature: 'Database', 
			Op: 'ExecuteProc', 
			Body: { 
				Proc: 'dcPing'
			} 
		}, function(resp) {
			if (resp.Result > 0) {
				console.log('error: ' + resp.Message);
				return;
			}
			
			console.log('Reply: ' + resp.Body.Text);
		});
	},
	Echo: function(text) {
		dc.comm.sendMessage({ 
			Service: 'dcCoreDataServices', 
			Feature: 'Database', 
			Op: 'ExecuteProc', 
			Body: { 
				Proc: 'dcEcho', 
				Params: { 
					Text: text 
				} 
			} 
		}, function(resp) {
			if (resp.Result > 0) {
				console.log('error: ' + resp.Message);
				return;
			}
			
			console.log('Reply: ' + resp.Body.Text);
		});
	},
	Encrypt: function(text) {
		dc.comm.sendMessage({ 
			Service: 'dcCoreDataServices', 
			Feature: 'Database', 
			Op: 'ExecuteProc', 
			Body: { 
				Proc: 'dcEncrypt', 
				Params: { 
					Value: text 
				} 
			} 
		}, function(resp) {
			if (resp.Result > 0) {
				console.log('error: ' + resp.Message);
				return;
			}
			
			console.log('Reply: ' + resp.Body.Value);
		});
	},
	Hash: function(text) {
		dc.comm.sendMessage({ 
			Service: 'dcCoreDataServices', 
			Feature: 'Database', 
			Op: 'ExecuteProc', 
			Body: { 
				Proc: 'dcHash', 
				Params: { 
					Value: text 
				} 
			} 
		}, function(resp) {
			if (resp.Result > 0) {
				console.log('error: ' + resp.Message);
				return;
			}
			
			console.log('Reply: ' + resp.Body.Value);
		});
	},
	Select: function(params) {

		/*
		 dcm.database.Select({ 
			Table: 'dcUser', 
			Select: [
				{
					Field: 'Id'
				},
				{
					Field: 'dcUsername'
				},
				{
					Field: 'dcFirstName'
				},
				{
					Field: 'dcLastName'
				}
			],
			Where: {
				Expression: 'StartsWith',
				A: {
					Field: 'dcEmail'
				},
				B: {
					Value: 'andy'
				}
			}
		 })
		 * 
		 *
		 * 
		 dcm.database.Select({ 
			Table: 'dcUser', 
			Select: [
				{
					Field: 'Id'
				},
				{
					Field: 'dgaZipPrefix',
					Name: 'ZipPrefix'
				},
				{
					Field: 'dgaDisplayName',
					Name: 'DisplayName'
				},
				{
					Field: 'dgaIntro',
					Name: 'Intro'
				},
				{
					Field: 'dgaImageSource',
					Name: 'ImageSource'
				},
				{
					Field: 'dgaImageDiscreet',
					Name: 'ImageDiscreet'
				},
				{
					Field: 'dcmState',
					Name: 'State'
				},
				{
					Field: 'dgaVisibleToList',
					Name: 'VisibleToList'
				},
				{
					Field: 'dcAuthorizationTag',
					Name: 'Badges'
				}
			],
			Where: {
				Expression: 'And',
				Children: [
					{
						Expression: 'Equal',
						A: {
							Field: 'dgaAccountState'
						},
						B: {
							Value: 'Active'
						}
					},
					{
						Expression: 'Equal',
						A: {
							Field: 'dcConfirmed'
						},
						B: {
							Value: true
						}
					}
				]
			},
			Collector: {
				Field: 'dcAuthorizationTag',
				Values: [ 'ApprenticeCandidate' ]
			}
		 })
		  
		<Request>
			<Field Name="Table" Type="dcTinyString" Required="True" />
			<Field Name="When" Type="dcTinyString" />
			<Field Name="Select">
				<List Type="dcDbSelectField" />
			</Field>
			<Field Name="Where" Type="dcDbWhereClause" />
			<Field Name="Collector">
				<Record>
					<Field Name="Func" Type="dcTinyString" />
					<!-- or -->
					<Field Name="Field" Type="dcTinyString" />
					<Field Name="SubId" Type="dcTinyString" />
					<Field Name="From" Type="Any" />
					<Field Name="To" Type="Any" />
					<Field Name="Values">
						<List Type="Any" />
					</Field>
					<Field Name="Extras" Type="AnyRecord" />
				</Record>
			</Field>
			<Field Name="Historical" Type="Boolean" />
		</Request>
		*/
		
		if (!params.Select)
			params.Select = [ ];  // select all
		
		dc.comm.sendMessage({ 
			Service: 'dcCoreDataServices', 
			Feature: 'Database', 
			Op: 'ExecuteProc', 
			Body: { 
				Proc: 'dcSelectDirect', 
				Params: params
			} 
		}, function(resp) {
			if (resp.Result > 0) {
				console.log('error: ' + resp.Message);
				return;
			}
			
			console.table(resp.Body);
		});
	},
	Insert: function(params) {

		/*
		 dcm.database.Insert({ 
			Table: 'dcUser', 
			Fields: {
				dcLocale: {
					Data: 'es',
					UpdateOnly: true
				},
				dcAuthorizationTag: {
					Admin: {
						Data: 'Admin'
					},
					Staff: {
						Data: 'Staff'
					}
				}
			}
		 })
		 
		*/
		
		dc.comm.sendMessage({ 
			Service: 'dcCoreDataServices', 
			Feature: 'Database', 
			Op: 'ExecuteProc', 
			Body: { 
				Proc: 'dcInsertRecord', 
				Params: params
			} 
		}, function(resp) {
			if (resp.Result > 0) {
				console.log('error: ' + resp.Message);
				return;
			}
			
			console.table(resp.Body);
		});
	},
	Update: function(params) {

		/*
		 dcm.database.Update({ 
			Table: 'dcUser', 
			Id: 'xxx',
			Fields: {
				dcLocale: {
					Data: 'es',
					UpdateOnly: true
				},
				dcAuthorizationTag: {
					Admin: {
						Data: 'Admin'
					},
					Staff: {
						Data: 'Staff'
					}
				}
			}
		 })
		 
		*/
		
		dc.comm.sendMessage({ 
			Service: 'dcCoreDataServices', 
			Feature: 'Database', 
			Op: 'ExecuteProc', 
			Body: { 
				Proc: 'dcUpdateRecord', 
				Params: params
			} 
		}, function(resp) {
			if (resp.Result > 0) {
				console.log('error: ' + resp.Message);
				return;
			}
			
			console.table(resp.Body);
		});
	},
	Retire: function(table, id) {
		dc.comm.sendMessage({ 
			Service: 'dcCoreDataServices', 
			Feature: 'Database', 
			Op: 'ExecuteProc', 
			Body: { 
				Proc: 'dcRetireRecord', 
				Params: { Table: table, Id: id }
			} 
		}, function(resp) {
			if (resp.Result > 0) {
				console.log('error: ' + resp.Message);
				return;
			}
			
			console.table(resp.Body);
		});
	}
}
