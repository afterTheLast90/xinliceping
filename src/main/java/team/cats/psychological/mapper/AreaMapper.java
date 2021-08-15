package team.cats.psychological.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.cats.psychological.entity.Area;

import java.util.List;

public interface AreaMapper extends BaseMapper<Area> {

    @Select("select * from area where area_principal = #{userId} ")
    public List<Area> selectByUserId(@Param("userId") Long userId);
}
