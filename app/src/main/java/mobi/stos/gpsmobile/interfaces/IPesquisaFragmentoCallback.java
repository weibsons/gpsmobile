package mobi.stos.gpsmobile.interfaces;

public interface IPesquisaFragmentoCallback {

    void onUpdatePageView(int index);

    void onCloseActivity();

    void onSavePesquisa();

    void onSaveFragment(int fragmentPosition);

}
