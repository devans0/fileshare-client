/**
 * title: Share Update Listener Interface
 * @author Dominic Evans
 * @date February 18, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

/**
 * This interface allows for registering listeners and is used by FSClientGUI to 
 * monitor for changes in the set of shared files. This allows the GUI to react
 * when this set of files changes whenever they occur.
 */

package fileshare.util;

@FunctionalInterface
public interface ShareUpdateListener {
	void onShareListChanged();
}