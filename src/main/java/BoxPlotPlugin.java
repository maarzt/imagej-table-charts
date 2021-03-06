import net.imagej.plot.AbstractPlot;
import net.imagej.plot.BoxSeries;
import net.imagej.plot.CategoryChart;
import net.imagej.plot.PlotService;
import net.imagej.table.Column;
import net.imagej.table.Table;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.display.Display;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import widgets.MultipleChoices;
import widgets.MutableChoices;

import java.util.*;

@Plugin(type = Command.class, menuPath="Table>BoxPlot")
public class BoxPlotPlugin implements Command {

	@Parameter(label = "Select table", callback = "tableChanged")
	private Table<Column<?>,?> table;

	@Parameter(label = "Select columns", initializer = "tableChanged")
	public MultipleChoices<Column<Double>> valueColumns;

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String LABEL_OPTIONAL = "optional";

	@Parameter(label = "Select key column", initializer = "tableChanged", required = false)
	public MutableChoices<Column<?>> keyColumn;

	@Parameter(label = "Boxplot", type = ItemIO.OUTPUT)
	public AbstractPlot output;

	@Parameter
	private PlotService plotService;

	@Override
	public void run() {
		createChart();
	}

	private void tableChanged() {
		if(keyColumn != null)
			keyColumn.setChoices(table, c -> c.getHeader());
		if(valueColumns != null) {
			Iterable<Column<Double>> choices = Utils.filterDoubleColumns(table);
			valueColumns.setChoices(choices, c -> c.getHeader());
			for(Column<Double> choice : choices)
				valueColumns.set(choice, true);
		}
	}

	private void createChart() {
		if (keyColumn != null)
			buildChartWithKeys();
		else
			buildChartWithoutKeys();
	}

	private void buildChartWithKeys() {
		output = ChartBuilderWithKey.build(plotService, keyColumn.get(), valueColumns.get());
		buildTitle();
	}

	private void buildChartWithoutKeys() {
		CategoryChart<String> chart = plotService.newCategoryChart(String.class);
		chart.numberAxis().setAutoRange();
		chart.categoryAxis().setLabel("Column");
		Map<String, Collection<Double>>	data = new TreeMap<>();
		for (Column<Double> valueColumn : valueColumns.get())
			data.put(valueColumn.getHeader(), valueColumn);
		BoxSeries<String> series = chart.addBoxSeries();
		series.setLabel("data");
		series.setLegendVisible(false);
		series.setValues(data);
		output = chart;
		buildTitle();
	}

	private void buildTitle() {
		String title = "Boxplot - ";
		for(Column<Double> col : valueColumns.get())
			title += col.getHeader() + ", ";
		output.setTitle(title.substring(0, title.length() - 2));
	}


	static private class ChartBuilderWithKey<K> {

		final PlotService plotService;

		private CategoryChart<K> chart;

		private ChartBuilderWithKey(PlotService plotService) {
			this.plotService = plotService;
		}

		private CategoryChart<K> getChart(Column<K> keyColumn, Iterable<Column<Double>> valueColumns) {
			chart = plotService.newCategoryChart(keyColumn.getType());
			chart.numberAxis().setAutoRange();
			chart.categoryAxis().setLabel(keyColumn.getHeader());
			for (Column<Double> valueColumn : valueColumns)
				addColumn(keyColumn, valueColumn);
			return chart;
		}

		private void addColumn(Column<K> keyColumn, Column<Double> valueColumn) {
			BoxSeries<K> series = chart.addBoxSeries();
			series.setLabel(valueColumn.getHeader());
			MyMultiMap<K, Double> map = new MyMultiMap<>(keyColumn, valueColumn);
			series.setValues(map.toMap());
		}

		static private <K> CategoryChart<K> build(PlotService plotService,
					Column<K> keyColumn, Iterable<Column<Double>> valueColumn) {
			return new ChartBuilderWithKey<K>(plotService).getChart(keyColumn, valueColumn);
		}
	}

	static private class MyMultiMap<K,V> {

		private Map<K, List<V>> map;

		public MyMultiMap(Collection<? extends K> keys, Collection<? extends V> values) {
			map = new HashMap<>();
			Iterator<? extends K> kIterator = keys.iterator();
			Iterator<? extends V> vIterator = values.iterator();
			while(kIterator.hasNext() && vIterator.hasNext())
				add(kIterator.next(), vIterator.next());
		}

		public void add(K k, V v) { get(k).add(v); }

		private List<V> get(K k) {
			List<V> list = map.get(k);
			if(list == null) {
				list = new ArrayList<>();
				map.put(k, list);
			}
			return list;
		}

		public Map<K, List<V>> toMap() { return map; }
	}

}
