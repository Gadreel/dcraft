import dcraft.hub.Hub;
import dcraft.tool.CounterFactory;

void CollectCounters(trun) {
	Hub.instance.getWorkPool().submit(CounterFactory.createCollectCounters());
				
	trun.complete();
}
