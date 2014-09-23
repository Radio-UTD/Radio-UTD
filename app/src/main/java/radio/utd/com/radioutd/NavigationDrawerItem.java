package radio.utd.com.radioutd;

import android.support.v4.app.Fragment;

/**
 * Created by Rahat on 9/22/2014.
 */
public class NavigationDrawerItem {
    // TODO: add icons to items
    private String title;
    private Class<? extends Fragment> fragmentClass;

    NavigationDrawerItem(String title, Class<? extends Fragment> fragmentClass)
    {
        this.title = title;
        this.fragmentClass = fragmentClass;
    }

    public String getTitle() {
        return title;
    }

    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }

    public Fragment getFragmentInstance() {
        try
        {
            return fragmentClass.newInstance();
        } catch(InstantiationException e) {
            return null;
        } catch(IllegalAccessException e) {
            return null;
        }
    }
}
