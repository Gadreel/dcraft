dc.test = {
	Task: {
		testParentTask: function(levels) {
			var count = Math.ceil(Math.random() * 10);
			var steps = [ ];
			
			for (var i = 0; i < count; i++) {
				steps.push({
					Alias: 'TopTask.' + i,
					Title: 'Top Task - ' + i,
					Params: {
						Levels: levels,
						Index: i
					},
					Func: function(step) {
						var task = this;
						
						console.log('running: ' + step.Title); 

						var sub = dc.test.Task.testParentSubTask(task, step, levels, Math.ceil(Math.random() * 10));			
						
						sub.run();
					}
				});
			}
			
			var subtask = new dc.lang.Task(steps);
						
			return subtask;
		},
		testParentSubTask: function(parenttask, parentstep, level, count) {
			var steps = [ ];
			
			for (var i = 0; i < count; i++) {
				steps.push({
					Alias: 'SubTask.' + level + '.' + i,
					Title: 'Sub Task - ' + level + ' - ' + i,
					Params: {
						Level: level,
						Index: i
					},
					Func: function(step) {
						var task = this;
						
						console.log('running: ' + step.Title); 
						
						setTimeout(function() { 
							console.log('progress: ' + step.Title); 
							
							step.Amount = 50;
							
							setTimeout(function() { 
								console.log('done: ' + step.Title); 
							
								task.resume();
							}, Math.ceil(Math.random() * 30) * 100)
						}, Math.ceil(Math.random() * 30) * 100)
					}
				});
			}
			
			var subtask = new dc.lang.Task(steps, function(res) {
				parenttask.resume();
			});
			
			subtask.ParentTask = parenttask;
			subtask.ParentStep = parentstep;
			
			if (! parentstep.Tasks)
				parentstep.Tasks = [ ];
				
			parentstep.Tasks.push(subtask);
						
			return subtask;
		}
	}
};