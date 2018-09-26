package mobi.stos.gpsmobile.bo;

import android.content.Context;

import java.util.List;

import mobi.stos.gpsmobile.bean.Position;
import mobi.stos.gpsmobile.dao.PositionDao;
import mobi.stos.podataka_lib.interfaces.IOperations;
import mobi.stos.podataka_lib.service.AbstractService;

public class PositionBo extends AbstractService<Position> {

    private PositionDao dao;

    public PositionBo(Context context) {
        super();
        this.dao = new PositionDao(context);
    }

    @Override
    protected IOperations<Position> getDao() {
        return dao;
    }

    public Position getNewest() {
        List<Position> positions = dao.list(null, null, "time DESC", 1);
        if (positions != null && !positions.isEmpty()) {
            return positions.get(0);
        }
        return null;
    }

}
