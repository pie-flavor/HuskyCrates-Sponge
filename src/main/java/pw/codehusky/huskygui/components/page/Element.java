package pw.codehusky.huskygui.components.page;

import org.spongepowered.api.item.inventory.ItemStack;

/**
 * An Element is simply an ItemStack relating to a Page state.
 * Elements do nothing by themselves, acting like a static, unmovable object.
 */
public class Element {
    private ItemStack displayItem;
    public ItemStack getDisplayItem() {
        return displayItem;
    }
}
