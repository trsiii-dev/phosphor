package dev.trsiii.phosphor.pages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The pages that exist, by name. */
public final class PageRegistry {

    private final Map<String, MfdPage> pages = new LinkedHashMap<>();

    public PageRegistry(List<MfdPage> pages) {
        for (MfdPage page : pages) {
            this.pages.put(page.name(), page);
        }
    }

    /** The pages built so far. Add a new page here. */
    public static PageRegistry standard() {
        return new PageRegistry(List.of(
                new LightsPage(), new FlightPage(), new VitalsPage(), new WaitingPage(), new DebugPage()));
    }

    public Optional<MfdPage> find(String name) {
        return Optional.ofNullable(pages.get(name));
    }
}
